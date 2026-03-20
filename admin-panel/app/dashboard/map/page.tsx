"use client";

/**
 * LiveMapPage — Admin module for real-time GPS tracking and ride simulation.
 *
 * Features:
 * - Live map showing all active bus positions from Firebase RTDB
 * - Multi-simulation: add multiple concurrent simulations, each pushes GPS data independently
 * - Adjustable update interval (1–30 seconds) per simulation
 * - Route-filtered bus selection: only buses assigned to the selected route are shown
 * - Active simulations panel showing all running sims with individual stop controls
 *
 * Data flow: Simulator → Firebase RTDB (`bus_locations/{busId}`) → LiveMap (real-time listener)
 */

import { useEffect, useState, useCallback, useMemo, useRef } from "react";
import dynamic from "next/dynamic";
import { collection, getDocs } from "firebase/firestore";
import { ref, set } from "firebase/database";
import { db, rtdb } from "@/lib/firebase";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Play, Square, Activity, Timer, Trash2, Bus, RefreshCcw } from "lucide-react";
import { toast } from "sonner";

// ─── Type Definitions ───────────────────────────────────────

/** Represents a route document in Firestore */
interface RouteNode {
    id: string;
    routeId?: string;
    name?: string;
    [key: string]: any;
}

/** Represents a bus document in Firestore */
interface BusNode {
    id: string;
    registrationNumber?: string;
    defaultRouteId?: string;
    [key: string]: any;
}

/** Represents an active simulation instance */
interface ActiveSimulation {
    id: string;               // Unique ID for this simulation instance
    busId: string;             // Bus document ID being simulated
    routeId: string;           // Route the bus is simulated on
    intervalMs: number;        // Update interval in milliseconds
    intervalHandle: NodeJS.Timeout; // Handle for clearing the setInterval
    currentPos: { lat: number; lng: number }; // Current simulated GPS position
    startedAt: Date;           // When this simulation was started
}

// ─── Dynamic Map Import ─────────────────────────────────────

/**
 * Leaflet requires the browser `window` object, so it must be loaded client-side only.
 * `dynamic()` with `ssr: false` ensures this component is never rendered on the server.
 */
const LiveMapNoSSR = dynamic(() => import("@/components/LiveMap"), {
    ssr: false,
    loading: () => (
        <div className="h-[600px] w-full rounded-xl border flex items-center justify-center bg-zinc-50 dark:bg-zinc-900">
            <div className="animate-pulse flex flex-col items-center">
                <div className="h-10 w-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4"></div>
                <p className="text-zinc-500 font-medium">Initializing Map Engine...</p>
            </div>
        </div>
    )
});

// ═══════════════════════════════════════════════════════════
// ─── RIDE SIMULATOR COMPONENT ───────────────────────────
// ═══════════════════════════════════════════════════════════

/**
 * RideSimulator — Developer tool for pushing simulated GPS data to Firebase RTDB.
 *
 * Supports multiple concurrent simulations. Each simulation:
 * - Writes to `bus_locations/{busId}` in RTDB at the configured interval
 * - Generates random lat/lng deltas to simulate bus movement around Colombo
 * - Runs independently and can be stopped individually or all at once
 */
function RideSimulator() {
    // ─── State ──────────────────────────────────────────────
    const [routes, setRoutes] = useState<RouteNode[]>([]);
    const [buses, setBuses] = useState<BusNode[]>([]);
    const [selectedRoute, setSelectedRoute] = useState("");
    const [selectedBus, setSelectedBus] = useState("");
    const [intervalSeconds, setIntervalSeconds] = useState("3");

    /** All currently running simulations */
    const [activeSimulations, setActiveSimulations] = useState<ActiveSimulation[]>([]);

    /** Ref to track simulations for cleanup (avoids stale closure issues) */
    const simulationsRef = useRef<ActiveSimulation[]>([]);

    // Keep ref in sync with state
    useEffect(() => {
        simulationsRef.current = activeSimulations;
    }, [activeSimulations]);

    // ─── Data Fetching ──────────────────────────────────────

    /** Fetches routes and buses from Firestore for the dropdown selectors */
    const fetchData = useCallback(async () => {
        try {
            // Fetch routes
            const routeSnap = await getDocs(collection(db, "routes"));
            const fetchedRoutes: RouteNode[] = [];
            routeSnap.forEach(doc => fetchedRoutes.push({ id: doc.id, ...doc.data() } as RouteNode));
            fetchedRoutes.sort((a, b) => (a.routeId || "").localeCompare(b.routeId || "", undefined, { numeric: true }));
            setRoutes(fetchedRoutes);

            // Fetch buses
            const busSnap = await getDocs(collection(db, "buses"));
            const fetchedBuses: BusNode[] = [];
            busSnap.forEach(doc => fetchedBuses.push({ id: doc.id, ...doc.data() } as BusNode));
            fetchedBuses.sort((a, b) => (a.registrationNumber || a.id).localeCompare(b.registrationNumber || b.id));
            setBuses(fetchedBuses);
        } catch (err) {
            console.error("Error fetching simulation data:", err);
            toast.error("Failed to load routes and buses.");
        }
    }, []);

    useEffect(() => { fetchData(); }, [fetchData]);

    // ─── Route-Filtered Buses ───────────────────────────────

    /**
     * Only shows buses whose `defaultRouteId` matches the selected route.
     * Also excludes buses that already have an active simulation running.
     */
    const availableBuses = useMemo(() => {
        if (!selectedRoute) return [];

        // Filter by route assignment
        const routeFiltered = buses.filter(b => b.defaultRouteId === selectedRoute);

        // Exclude buses already being simulated
        const activeBusIds = new Set(activeSimulations.map(s => s.busId));
        return routeFiltered.filter(b => !activeBusIds.has(b.id));
    }, [buses, selectedRoute, activeSimulations]);

    // ─── Simulation Handlers ────────────────────────────────

    /**
     * Starts a new simulation for the selected bus on the selected route.
     * Each simulation pushes GPS coordinates to RTDB at the configured interval.
     * Multiple simulations can run concurrently for different buses.
     */
    const startSimulation = () => {
        if (!selectedRoute || !selectedBus) {
            toast.error("Please select a route and a bus first.");
            return;
        }

        const interval = Math.max(1, Math.min(30, parseInt(intervalSeconds) || 3));
        const intervalMs = interval * 1000;

        // Starting position: Colombo Fort area with slight random offset per simulation
        const startPos = {
            lat: 6.9271 + (Math.random() - 0.5) * 0.01,
            lng: 79.8612 + (Math.random() - 0.5) * 0.01,
        };

        // Mutable position tracker for this simulation instance
        const posRef = { ...startPos };

        const busId = selectedBus;
        const routeId = selectedRoute;

        // Create the interval that pushes GPS data to RTDB
        const handle = setInterval(() => {
            // Random movement delta (biased slightly south-east to simulate a route)
            const latDelta = (Math.random() - 0.3) * 0.004;
            const lngDelta = (Math.random() - 0.3) * 0.004;
            posRef.lat += latDelta;
            posRef.lng += lngDelta;

            // Write to Firebase RTDB: `bus_locations/{busId}`
            const busRef = ref(rtdb, `bus_locations/${busId}`);
            set(busRef, {
                lat: posRef.lat,
                lng: posRef.lng,
                routeId: routeId,
                speed: Math.floor(Math.random() * 35) + 15,       // 15-50 km/h
                crowdLevel: ["Low", "Medium", "High"][Math.floor(Math.random() * 3)],
                lastUpdated: Math.floor(Date.now() / 1000),        // Unix timestamp
            }).catch(console.error);
        }, intervalMs);

        // Create simulation record
        const sim: ActiveSimulation = {
            id: `${busId}_${Date.now()}`,
            busId,
            routeId,
            intervalMs,
            intervalHandle: handle,
            currentPos: startPos,
            startedAt: new Date(),
        };

        setActiveSimulations(prev => [...prev, sim]);
        toast.info(`🚌 Simulation started for ${busId} — updating every ${interval}s`);

        // Reset form for next simulation
        setSelectedBus("");
    };

    /**
     * Stops a specific simulation by its ID.
     * Clears the interval and removes it from the active list.
     */
    const stopSimulation = (simId: string) => {
        setActiveSimulations(prev => {
            const sim = prev.find(s => s.id === simId);
            if (sim) {
                clearInterval(sim.intervalHandle);
                toast.success(`🛑 Simulation stopped for ${sim.busId}`);
            }
            return prev.filter(s => s.id !== simId);
        });
    };

    /** Stops all running simulations at once */
    const stopAllSimulations = () => {
        activeSimulations.forEach(sim => clearInterval(sim.intervalHandle));
        setActiveSimulations([]);
        toast.success("🛑 All simulations stopped.");
    };

    /** Clean up all intervals when component unmounts */
    useEffect(() => {
        return () => {
            simulationsRef.current.forEach(sim => clearInterval(sim.intervalHandle));
        };
    }, []);

    // ─── Helpers ────────────────────────────────────────────

    /** Format elapsed time since simulation started */
    const formatElapsed = (startedAt: Date): string => {
        const seconds = Math.floor((Date.now() - startedAt.getTime()) / 1000);
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return mins > 0 ? `${mins}m ${secs}s` : `${secs}s`;
    };

    // ─── Tick for elapsed time display ──────────────────────
    const [, setTick] = useState(0);
    useEffect(() => {
        if (activeSimulations.length === 0) return;
        const ticker = setInterval(() => setTick(t => t + 1), 1000);
        return () => clearInterval(ticker);
    }, [activeSimulations.length]);

    // ═══════════════════════════════════════════════════════════
    // ─── RENDER ─────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════

    return (
        <Card className="border-blue-100 dark:border-blue-900/30 shadow-sm">
            {/* ─── Header ────────────────────────────────────── */}
            <CardHeader className="bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-950/40 dark:to-indigo-950/40 border-b border-blue-100 dark:border-blue-900/30">
                <CardTitle className="flex items-center gap-2 text-lg">
                    <Activity className="h-5 w-5 text-blue-600" />
                    Developer Ride Simulator
                    {activeSimulations.length > 0 && (
                        <Badge variant="secondary" className="ml-2">
                            {activeSimulations.length} active
                        </Badge>
                    )}
                </CardTitle>
                <CardDescription>
                    Push simulated GPS coordinates to Firebase RTDB. Multiple simulations can run concurrently.
                </CardDescription>
            </CardHeader>

            <CardContent className="pt-6 space-y-6">
                {/* ─── Configuration Form ────────────────────── */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    {/* Step 1: Route */}
                    <div className="space-y-2">
                        <Label className="text-sm font-semibold text-zinc-700 dark:text-zinc-300">
                            Step 1 — Route
                        </Label>
                        <Select
                            value={selectedRoute}
                            onValueChange={(val) => {
                                setSelectedRoute(val);
                                setSelectedBus(""); // Reset bus when route changes
                            }}
                        >
                            <SelectTrigger className="bg-white dark:bg-zinc-950">
                                <SelectValue placeholder="Choose a route..." />
                            </SelectTrigger>
                            <SelectContent>
                                {routes.length === 0 && (
                                    <SelectItem value="_none" disabled>No routes found</SelectItem>
                                )}
                                {routes.map(r => (
                                    <SelectItem key={r.id} value={r.id}>
                                        {r.routeId || r.id} — {r.name || ""}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Step 2: Bus (filtered by selected route) */}
                    <div className="space-y-2">
                        <Label className="text-sm font-semibold text-zinc-700 dark:text-zinc-300">
                            Step 2 — Bus
                        </Label>
                        <Select
                            value={selectedBus}
                            onValueChange={setSelectedBus}
                            disabled={!selectedRoute}
                        >
                            <SelectTrigger className="bg-white dark:bg-zinc-950">
                                <SelectValue placeholder={selectedRoute ? "Choose a bus..." : "Select route first"} />
                            </SelectTrigger>
                            <SelectContent>
                                {availableBuses.length === 0 && (
                                    <SelectItem value="_none" disabled>
                                        {selectedRoute ? "No available buses on this route" : "Select a route first"}
                                    </SelectItem>
                                )}
                                {availableBuses.map(b => (
                                    <SelectItem key={b.id} value={b.id}>
                                        {b.registrationNumber || b.id}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Step 3: Update interval (seconds) */}
                    <div className="space-y-2">
                        <Label className="text-sm font-semibold text-zinc-700 dark:text-zinc-300">
                            <div className="flex items-center gap-1">
                                <Timer className="h-3.5 w-3.5" />
                                Interval (sec)
                            </div>
                        </Label>
                        <Input
                            type="number"
                            min={1}
                            max={30}
                            value={intervalSeconds}
                            onChange={(e) => setIntervalSeconds(e.target.value)}
                            className="bg-white dark:bg-zinc-950"
                            placeholder="3"
                        />
                    </div>

                    {/* Start button */}
                    <div className="space-y-2">
                        <Label className="text-sm font-semibold text-transparent select-none">Action</Label>
                        <Button
                            onClick={startSimulation}
                            disabled={!selectedRoute || !selectedBus}
                            className="bg-blue-600 hover:bg-blue-700 text-white w-full"
                        >
                            <Play className="h-4 w-4 mr-2 fill-white" />
                            Start Simulation
                        </Button>
                    </div>
                </div>

                {/* ─── Active Simulations Panel ──────────────── */}
                {activeSimulations.length > 0 && (
                    <div className="space-y-3">
                        <div className="flex items-center justify-between">
                            <h4 className="text-sm font-semibold text-zinc-700 dark:text-zinc-300">
                                Active Simulations
                            </h4>
                            {activeSimulations.length > 1 && (
                                <Button
                                    variant="outline"
                                    size="sm"
                                    className="text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950 border-red-200 dark:border-red-900"
                                    onClick={stopAllSimulations}
                                >
                                    <Square className="h-3 w-3 mr-1 fill-red-500" />
                                    Stop All
                                </Button>
                            )}
                        </div>

                        <div className="space-y-2">
                            {activeSimulations.map(sim => (
                                <div
                                    key={sim.id}
                                    className="flex items-center justify-between rounded-lg bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800 px-4 py-3"
                                >
                                    <div className="flex items-center gap-3">
                                        {/* Pulsing live indicator */}
                                        <span className="relative flex h-2.5 w-2.5">
                                            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-500 opacity-75"></span>
                                            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-blue-600"></span>
                                        </span>
                                        <div>
                                            <p className="text-sm font-medium text-blue-700 dark:text-blue-300">
                                                <Bus className="h-3.5 w-3.5 inline mr-1" />
                                                <span className="font-bold">{sim.busId}</span>
                                                <span className="mx-2 text-blue-400">→</span>
                                                Route {sim.routeId}
                                            </p>
                                            <p className="text-xs text-blue-500 dark:text-blue-400">
                                                Every {sim.intervalMs / 1000}s • Running for {formatElapsed(sim.startedAt)}
                                            </p>
                                        </div>
                                    </div>
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        className="h-8 w-8 text-red-500 hover:text-red-600 hover:bg-red-100 dark:hover:bg-red-950/50"
                                        onClick={() => stopSimulation(sim.id)}
                                    >
                                        <Square className="h-4 w-4 fill-red-500" />
                                    </Button>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}

// ═══════════════════════════════════════════════════════════
// ─── PAGE COMPONENT ─────────────────────────────────────
// ═══════════════════════════════════════════════════════════

/**
 * LiveMapPage — Main page that composes the live map and ride simulator.
 * The map shows all bus markers from RTDB in real-time.
 * The simulator pushes test GPS data to RTDB for development/testing.
 */
export default function LiveMapPage() {
    return (
        <div className="space-y-6">
            {/* ─── Page Header ───────────────────────────────── */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">Live Network Monitoring</h2>
                    <p className="text-muted-foreground mt-1">Real-time GPS tracking and ride simulation.</p>
                </div>
                <Button variant="outline" size="sm" onClick={() => window.location.reload()}>
                    <RefreshCcw className="h-4 w-4 mr-2" />
                    Refresh
                </Button>
            </div>

            {/* ─── Live Map Card ──────────────────────────────── */}
            <Card>
                <CardHeader>
                    <CardTitle>Fleet GPS Radar</CardTitle>
                    <CardDescription>
                        Real-time tracking of all active transit vehicles via Firebase RTDB listeners.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <LiveMapNoSSR />
                </CardContent>
            </Card>

            {/* ─── Ride Simulator ─────────────────────────────── */}
            <RideSimulator />
        </div>
    );
}
