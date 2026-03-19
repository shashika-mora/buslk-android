"use client";

/**
 * TripsPage — Admin module for global Trip Ledger & Financials.
 *
 * This page provides a global view of all recent network trips,
 * status tracking (Ongoing, Completed, Cancelled), and financial
 * details (fares, distances).
 *
 * Optimizations implemented:
 * - Removed 'Rs' symbol and used text 'Rs' as requested.
 * - Added comprehensive JSDoc comments and TypeScript interfaces.
 * - Implemented actual Firestore wallet balance adjustment logic.
 * - Created lookup maps to display human-readable names for Users, Buses, and Routes.
 */

import { useEffect, useState, useMemo, useCallback } from "react";
import { collection, query, orderBy, getDocs, limit, doc, updateDoc, setDoc, deleteDoc, getDoc, serverTimestamp, increment, addDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Navigation, Search, Wallet, Plus, Edit2, Trash2, Bus as BusIcon, User, RefreshCcw } from "lucide-react";
import { toast } from "sonner";
import { format } from "date-fns";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

// ─── Type Definitions ───────────────────────────────────────

interface TripRecord {
    id: string;
    userId: string;
    busId: string;
    routeId: string;
    status: string;
    startLocationName: string;
    endLocationName: string;
    distanceKm: number;
    totalFare: number;
    pointsEarned: number;
    startTime: any;
    endTime?: any;
}

interface UserData {
    id: string;
    displayName?: string;
    email?: string;
}

interface BusData {
    id: string;
    registrationNumber?: string;
    routeId?: string;
    defaultRouteId?: string;
}

interface RouteData {
    id: string;
    name?: string;
    startLocationName?: string;
    endLocationName?: string;
}

// ═══════════════════════════════════════════════════════════
// ─── TRIPS COMPONENT ───────────────────────────────────────
// ═══════════════════════════════════════════════════════════

export default function TripsPage() {
    // ─── State ──────────────────────────────────────────────
    const [trips, setTrips] = useState<TripRecord[]>([]);
    const [filteredTrips, setFilteredTrips] = useState<TripRecord[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState("");

    // Metadata states for resolving IDs
    const [routes, setRoutes] = useState<RouteData[]>([]);
    const [buses, setBuses] = useState<BusData[]>([]);
    const [users, setUsers] = useState<UserData[]>([]);

    // Wallet adjustment state
    const [isAdjustingWallet, setIsAdjustingWallet] = useState(false);
    const [isSubmittingWallet, setIsSubmittingWallet] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
    const [adjustmentAmount, setAdjustmentAmount] = useState("");
    const [adjustmentReason, setAdjustmentReason] = useState("");

    // Trip CRUD state
    const [isTripDialogOpen, setIsTripDialogOpen] = useState(false);
    const [isSubmittingTrip, setIsSubmittingTrip] = useState(false);
    const [editingTripId, setEditingTripId] = useState<string | null>(null);
    const [tripFormData, setTripFormData] = useState({
        userId: "",
        busId: "",
        routeId: "",
        status: "COMPLETED",
        startLocationName: "",
        endLocationName: "",
        distanceKm: 0,
        totalFare: 0,
        pointsEarned: 0,
    });

    // ─── Lookup Maps ────────────────────────────────────────

    /** Resolve User ID to Display Name or Email */
    const userMap = useMemo(() => {
        const map: Record<string, string> = {};
        users.forEach(u => { map[u.id] = u.displayName || u.email || "Passenger"; });
        return map;
    }, [users]);

    /** Resolve Bus ID to Registration Number */
    const busMap = useMemo(() => {
        const map: Record<string, string> = {};
        buses.forEach(b => { map[b.id] = b.registrationNumber || b.id; });
        return map;
    }, [buses]);

    /** Resolve Route ID to readable route name */
    const routeMap = useMemo(() => {
        const map: Record<string, string> = {};
        routes.forEach(r => {
            map[r.id] = r.name || r.id;
        });
        return map;
    }, [routes]);


    // ─── Data Fetching ──────────────────────────────────────

    const fetchTrips = useCallback(async () => {
        try {
            setLoading(true);
            const q = query(
                collection(db, "trips"),
                orderBy("startTime", "desc"),
                limit(100)
            );
            const querySnapshot = await getDocs(q);

            const fetched: TripRecord[] = [];
            querySnapshot.forEach((doc) => {
                fetched.push({ id: doc.id, ...doc.data() } as TripRecord);
            });

            setTrips(fetched);
            setFilteredTrips(fetched);
        } catch (error) {
            console.error("Error fetching trips:", error);
            // Fallback for missing composite indexes
            try {
                const qFB = query(collection(db, "trips"), limit(100));
                const qsFB = await getDocs(qFB);
                const fetchedFB: TripRecord[] = [];
                qsFB.forEach((doc) => {
                    fetchedFB.push({ id: doc.id, ...doc.data() } as TripRecord);
                });
                fetchedFB.sort((a, b) => {
                    const timeA = a.startTime ? new Date(a.startTime).getTime() : 0;
                    const timeB = b.startTime ? new Date(b.startTime).getTime() : 0;
                    return timeB - timeA;
                });
                setTrips(fetchedFB);
                setFilteredTrips(fetchedFB);
            } catch (err2) {
                toast.error("Failed to load global trips.");
            }
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchTrips();

        // Fetch metadata concurrently
        const fetchMetadata = async () => {
            try {
                const [rSnap, bSnap, uSnap] = await Promise.all([
                    getDocs(collection(db, "routes")),
                    getDocs(collection(db, "buses")),
                    getDocs(collection(db, "users"))
                ]);

                const rList: RouteData[] = [];
                rSnap.forEach(d => rList.push({ id: d.id, ...d.data() } as RouteData));
                setRoutes(rList);

                const bList: BusData[] = [];
                bSnap.forEach(d => bList.push({ id: d.id, ...d.data() } as BusData));
                setBuses(bList);

                const uList: UserData[] = [];
                uSnap.forEach(d => uList.push({ id: d.id, ...d.data() } as UserData));
                setUsers(uList);
            } catch (err) {
                console.error("Error fetching metadata for trips page:", err);
            }
        };

        fetchMetadata();
    }, [fetchTrips]);

    // ─── Local Search Filter ────────────────────────────────

    useEffect(() => {
        if (!searchTerm) {
            setFilteredTrips(trips);
            return;
        }
        const lowerSearch = searchTerm.toLowerCase();

        const filtered = trips.filter(t => {
            const userName = userMap[t.userId]?.toLowerCase() || "";
            const busReg = busMap[t.busId]?.toLowerCase() || "";

            return (
                t.id.toLowerCase().includes(lowerSearch) ||
                (t.userId && t.userId.toLowerCase().includes(lowerSearch)) ||
                (t.busId && t.busId.toLowerCase().includes(lowerSearch)) ||
                (t.routeId && t.routeId.toLowerCase().includes(lowerSearch)) ||
                userName.includes(lowerSearch) ||
                busReg.includes(lowerSearch)
            );
        });
        setFilteredTrips(filtered);
    }, [searchTerm, trips, userMap, busMap]);


    // ─── Wallet Adjustment Logic ────────────────────────────

    /**
     * Submits a manual wallet adjustment for a specific user to Firestore.
     * Updates the user's document and creates a transaction record.
     */
    const handleAdjustWalletSubmit = async () => {
        if (!selectedUserId || !adjustmentAmount) {
            toast.error("Please provide both user ID and adjustment amount.");
            return;
        }

        const amount = parseFloat(adjustmentAmount);
        if (isNaN(amount)) {
            toast.error("Invalid amount.");
            return;
        }

        try {
            setIsSubmittingWallet(true);
            const userRef = doc(db, "users", selectedUserId);
            const userSnap = await getDoc(userRef);

            if (!userSnap.exists()) {
                toast.error("User not found in system.");
                return;
            }

            // Update user balance
            await updateDoc(userRef, {
                walletBalance: increment(amount),
                lastWalletUpdate: serverTimestamp()
            });

            // Log transaction
            await addDoc(collection(db, "transactions"), {
                userId: selectedUserId,
                amount: amount,
                reason: adjustmentReason || "Manual Admin Adjustment",
                type: amount > 0 ? "CREDIT" : "DEBIT",
                timestamp: serverTimestamp(),
                performedBy: "admin"
            });

            toast.success(`Wallet successfully updated: ${amount > 0 ? '+' : ''}${amount} Rs`);
            setIsAdjustingWallet(false);
            setAdjustmentAmount("");
            setAdjustmentReason("");
            setSelectedUserId(null);
        } catch (error: any) {
            console.error("Error adjusting wallet:", error);
            toast.error("Failed to adjust wallet: " + error.message);
        } finally {
            setIsSubmittingWallet(false);
        }
    };


    // ─── Trip CRUD Handlers ─────────────────────────────────

    const handleOpenTripDialog = (tripToEdit?: any) => {
        if (tripToEdit) {
            setEditingTripId(tripToEdit.id);
            setTripFormData({
                userId: tripToEdit.userId || "",
                busId: tripToEdit.busId || "",
                routeId: tripToEdit.routeId || "",
                status: tripToEdit.status || "COMPLETED",
                startLocationName: tripToEdit.startLocationName || "",
                endLocationName: tripToEdit.endLocationName || "",
                distanceKm: tripToEdit.distanceKm || 0,
                totalFare: tripToEdit.totalFare || 0,
                pointsEarned: tripToEdit.pointsEarned || 0,
            });
        } else {
            setEditingTripId(null);
            setTripFormData({
                userId: "",
                busId: "",
                routeId: "",
                status: "COMPLETED",
                startLocationName: "Colombo",
                endLocationName: "Kandy",
                distanceKm: 115,
                totalFare: 550,
                pointsEarned: 25,
            });
        }
        setIsTripDialogOpen(true);
    };

    const handleSaveTrip = async () => {
        if (!tripFormData.userId || !tripFormData.busId) {
            toast.error("User ID and Bus ID are required.");
            return;
        }

        try {
            setIsSubmittingTrip(true);

            const tripData = {
                userId: tripFormData.userId,
                busId: tripFormData.busId,
                routeId: tripFormData.routeId,
                status: tripFormData.status,
                startLocationName: tripFormData.startLocationName,
                endLocationName: tripFormData.endLocationName,
                distanceKm: Number(tripFormData.distanceKm),
                totalFare: Number(tripFormData.totalFare),
                pointsEarned: Number(tripFormData.pointsEarned),
                // Only set startTime if it's a new record
                ...(editingTripId ? {} : { startTime: serverTimestamp(), endTime: serverTimestamp() })
            };

            if (editingTripId) {
                await updateDoc(doc(db, "trips", editingTripId), tripData);
                toast.success("Trip record updated.");
            } else {
                const newDocRef = doc(collection(db, "trips"));
                await setDoc(newDocRef, tripData);

                // Auto-update user stats if trip is COMPLETED
                if (tripFormData.status === "COMPLETED" && tripFormData.userId) {
                    const userRef = doc(db, "users", tripFormData.userId);
                    const userSnap = await getDoc(userRef);
                    if (userSnap.exists()) {
                        await updateDoc(userRef, {
                            points: increment(Number(tripFormData.pointsEarned) || 0),
                            "stats.totalTrips": increment(1),
                            "stats.totalDistanceKm": increment(Number(tripFormData.distanceKm) || 0),
                        });
                        toast.info(`User stats updated: +${tripFormData.pointsEarned} pts, +${tripFormData.distanceKm} km`);
                    }
                }

                toast.success("New trip created.");
            }

            setIsTripDialogOpen(false);
            fetchTrips();
        } catch (error) {
            console.error("Error saving trip:", error);
            toast.error("Failed to save trip data.");
        } finally {
            setIsSubmittingTrip(false);
        }
    };

    const handleDeleteTrip = async (id: string, e: React.MouseEvent) => {
        e.stopPropagation(); // Prevents row click if we had one
        if (!confirm("Are you sure you want to delete this trip record? This affects analytics.")) return;

        try {
            // Read the trip first so we can reverse the user's stats
            const tripSnap = await getDoc(doc(db, "trips", id));
            const tripData = tripSnap.exists() ? tripSnap.data() : null;

            await deleteDoc(doc(db, "trips", id));

            // Reverse user stats if the trip was COMPLETED
            if (tripData && tripData.status === "COMPLETED" && tripData.userId) {
                const userRef = doc(db, "users", tripData.userId);
                const userSnap = await getDoc(userRef);
                if (userSnap.exists()) {
                    await updateDoc(userRef, {
                        points: increment(-(Number(tripData.pointsEarned) || 0)),
                        "stats.totalTrips": increment(-1),
                        "stats.totalDistanceKm": increment(-(Number(tripData.distanceKm) || 0)),
                    });
                    toast.info("User stats reversed.");
                }
            }

            toast.success("Trip record deleted.");
            fetchTrips();
        } catch (error) {
            console.error("Error deleting trip:", error);
            toast.error("Failed to delete trip.");
        }
    };

    // ─── Helpers ────────────────────────────────────────────

    const getStatusBadge = (status: string) => {
        switch (status?.toUpperCase()) {
            case 'COMPLETED':
                return <Badge variant="default" className="bg-green-600 hover:bg-green-700">Completed</Badge>;
            case 'ONGOING':
                return <Badge variant="secondary" className="bg-blue-100 text-blue-800 hover:bg-blue-200 dark:bg-blue-900/30 dark:text-blue-300">Ongoing</Badge>;
            case 'CANCELLED':
                return <Badge variant="destructive">Cancelled</Badge>;
            default:
                return <Badge variant="outline">{status || 'UNKNOWN'}</Badge>;
        }
    };

    // ═══════════════════════════════════════════════════════════
    // ─── RENDER ─────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════

    return (
        <div className="space-y-6">
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center space-y-4 md:space-y-0">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">Trip Ledger</h2>
                    <p className="text-muted-foreground mt-1">Global view of all recent network trips and financial transactions.</p>
                </div>

                <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
                    <div className="relative flex-1 md:w-64">
                        <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-zinc-500" />
                        <Input
                            type="search"
                            placeholder="Search by ID, User, Bus..."
                            className="pl-8 bg-white dark:bg-zinc-950"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <Button variant="outline" onClick={fetchTrips} disabled={loading} className="whitespace-nowrap">
                        <RefreshCcw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                        Refresh
                    </Button>
                    <Button variant="outline" onClick={() => handleOpenTripDialog()} className="whitespace-nowrap">
                        <Plus className="h-4 w-4 mr-2" />
                        Inject Trip
                    </Button>
                    <Button
                        variant="default"
                        onClick={() => {
                            setSelectedUserId("");
                            setIsAdjustingWallet(true);
                        }}
                        className="whitespace-nowrap"
                    >
                        <Wallet className="h-4 w-4 mr-2" />
                        Adjust Wallet
                    </Button>
                </div>
            </div>

            <Card className="overflow-hidden border-zinc-200 dark:border-zinc-800 shadow-sm">
                <div className="overflow-x-auto">
                    <Table>
                        <TableHeader className="bg-zinc-50 dark:bg-zinc-900/50">
                            <TableRow>
                                <TableHead className="w-[100px]">Status</TableHead>
                                <TableHead>Date & Time</TableHead>
                                <TableHead>Entities</TableHead>
                                <TableHead>Journey</TableHead>
                                <TableHead className="text-right">Distance</TableHead>
                                <TableHead className="text-right">Fare</TableHead>
                                <TableHead className="w-[100px] text-right">Actions</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {loading ? (
                                <TableRow>
                                    <TableCell colSpan={7} className="h-24 text-center text-zinc-500">
                                        Loading ledger records...
                                    </TableCell>
                                </TableRow>
                            ) : filteredTrips.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={7} className="h-24 text-center text-zinc-500">
                                        No trips found.
                                    </TableCell>
                                </TableRow>
                            ) : (
                                filteredTrips.map((trip) => (
                                    <TableRow key={trip.id} className="hover:bg-zinc-50 dark:hover:bg-zinc-900/50 group">
                                        <TableCell>
                                            {getStatusBadge(trip.status)}
                                            {trip.pointsEarned > 0 && (
                                                <div className="text-[10px] text-green-600 dark:text-green-400 font-medium mt-1">
                                                    +{trip.pointsEarned} pts
                                                </div>
                                            )}
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex flex-col text-sm">
                                                <span className="font-medium">
                                                    {trip.startTime
                                                        ? (typeof trip.startTime.toDate === 'function'
                                                            ? format(trip.startTime.toDate(), 'MMM d, yyyy')
                                                            : (!isNaN(new Date(trip.startTime).getTime()) ? format(new Date(trip.startTime), 'MMM d, yyyy') : "Invalid Date"))
                                                        : "N/A"}
                                                </span>
                                                <span className="text-zinc-500 text-xs mt-0.5">
                                                    {trip.startTime
                                                        ? (typeof trip.startTime.toDate === 'function'
                                                            ? format(trip.startTime.toDate(), 'h:mm a')
                                                            : (!isNaN(new Date(trip.startTime).getTime()) ? format(new Date(trip.startTime), 'h:mm a') : ""))
                                                        : ""}
                                                    {trip.endTime ? " - " + (typeof trip.endTime.toDate === 'function' ? format(trip.endTime.toDate(), 'h:mm a') : (!isNaN(new Date(trip.endTime).getTime()) ? format(new Date(trip.endTime), 'h:mm a') : "")) : ""}
                                                </span>
                                                <span className="text-[10px] text-zinc-400 font-mono mt-1 hidden lg:block" title={trip.id}>{trip.id.substring(0, 10)}...</span>
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex flex-col space-y-1.5 text-sm">
                                                <span className="flex items-center text-zinc-900 dark:text-zinc-100 font-medium truncate max-w-[150px]" title={trip.userId}>
                                                    <User className="h-3 w-3 mr-1.5 text-zinc-500" />
                                                    {userMap[trip.userId] || trip.userId || "Unknown User"}
                                                </span>
                                                <span className="flex items-center text-zinc-600 dark:text-zinc-400 text-xs">
                                                    <BusIcon className="h-3 w-3 mr-1.5 text-zinc-500" />
                                                    {busMap[trip.busId] || trip.busId || "N/A"}
                                                </span>
                                                <span className="flex items-center text-zinc-600 dark:text-zinc-400 text-xs">
                                                    <Navigation className="h-3 w-3 mr-1.5 text-zinc-500" />
                                                    {routeMap[trip.routeId] || trip.routeId || "N/A"}
                                                </span>
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex flex-col space-y-1.5 text-sm max-w-[200px]">
                                                <div className="flex items-start">
                                                    <div className="mt-1 mr-2 h-2 w-2 rounded-full bg-green-500 flex-shrink-0" />
                                                    <span className="truncate" title={trip.startLocationName || "Unknown Origin"}>{trip.startLocationName || "Unknown Start"}</span>
                                                </div>
                                                <div className="flex items-start">
                                                    <div className="mt-1 mr-2 h-2 w-2 rounded-full bg-red-500 flex-shrink-0" />
                                                    <span className="truncate text-zinc-500" title={trip.endLocationName || (trip.status === "ONGOING" ? "In Progress..." : "Unknown Destination")}>
                                                        {trip.endLocationName || (trip.status === "ONGOING" ? "In Progress..." : "Unknown Dest")}
                                                    </span>
                                                </div>
                                            </div>
                                        </TableCell>
                                        <TableCell className="text-right">
                                            {trip.distanceKm ? (
                                                <span className="text-sm font-medium">{Number(trip.distanceKm).toFixed(1)} km</span>
                                            ) : (
                                                <span className="text-sm text-zinc-500">-</span>
                                            )}
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <div className="flex flex-col items-end">
                                                <span className="font-semibold text-zinc-900 dark:text-zinc-50 flex items-center">
                                                    <span className="text-zinc-500 text-xs mr-1 font-normal">Rs</span>
                                                    {trip.totalFare ? Number(trip.totalFare).toFixed(2) : "0.00"}
                                                </span>
                                            </div>
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <div className="flex items-center justify-end space-x-1 opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity">
                                                <Button variant="ghost" size="icon" className="h-8 w-8 text-zinc-500 hover:text-zinc-900 dark:hover:text-white" onClick={() => handleOpenTripDialog(trip)}>
                                                    <Edit2 className="h-4 w-4" />
                                                </Button>
                                                <Button variant="ghost" size="icon" className="h-8 w-8 text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20" onClick={(e) => handleDeleteTrip(trip.id, e)}>
                                                    <Trash2 className="h-4 w-4" />
                                                </Button>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </div>
            </Card>

            {/* Trip CRUD Dialog */}
            <Dialog open={isTripDialogOpen} onOpenChange={setIsTripDialogOpen}>
                <DialogContent className="sm:max-w-[500px]">
                    <DialogHeader>
                        <DialogTitle>{editingTripId ? "Edit Trip Record" : "Inject Mock Trip"}</DialogTitle>
                        <DialogDescription>
                            Manually create or edit a trip journey record in the ledger.
                        </DialogDescription>
                    </DialogHeader>

                    <div className="grid gap-4 py-4 max-h-[60vh] overflow-y-auto pr-2">
                        <div className="grid grid-cols-2 gap-4">
                            <div className="grid gap-2">
                                <Label htmlFor="userId">User</Label>
                                <Select value={tripFormData.userId} onValueChange={(val) => setTripFormData({ ...tripFormData, userId: val })}>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select a user..." />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="MOCK_USER">MOCK_USER</SelectItem>
                                        {users.map(u => (
                                            <SelectItem key={u.id} value={u.id}>
                                                {u.displayName || u.email || u.id}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                            <div className="grid gap-2">
                                <Label htmlFor="routeId">Route</Label>
                                <Select value={tripFormData.routeId} onValueChange={(val) => setTripFormData({ ...tripFormData, routeId: val, busId: "" })}>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select a route..." />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {routes.map(r => (
                                            <SelectItem key={r.id} value={r.id}>
                                                {r.name || r.id}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="grid gap-2">
                                <Label htmlFor="busId">Bus</Label>
                                <Select value={tripFormData.busId} onValueChange={(val) => setTripFormData({ ...tripFormData, busId: val })} disabled={!tripFormData.routeId}>
                                    <SelectTrigger>
                                        <SelectValue placeholder={tripFormData.routeId ? "Select a bus..." : "Select route first"} />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {buses
                                            .filter(b => !tripFormData.routeId || b.defaultRouteId === tripFormData.routeId || b.routeId === tripFormData.routeId)
                                            .map(b => (
                                                <SelectItem key={b.id} value={b.id}>
                                                    {b.registrationNumber || b.id}
                                                </SelectItem>
                                            ))
                                        }
                                        {buses.filter(b => !tripFormData.routeId || b.defaultRouteId === tripFormData.routeId || b.routeId === tripFormData.routeId).length === 0 && (
                                            <SelectItem value="_none" disabled>No buses on this route</SelectItem>
                                        )}
                                    </SelectContent>
                                </Select>
                            </div>
                            <div className="grid gap-2">
                                <Label htmlFor="status">Trip Status</Label>
                                <Select value={tripFormData.status} onValueChange={(val) => setTripFormData({ ...tripFormData, status: val })}>
                                    <SelectTrigger id="status">
                                        <SelectValue placeholder="Status" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="ONGOING">Ongoing</SelectItem>
                                        <SelectItem value="COMPLETED">Completed</SelectItem>
                                        <SelectItem value="CANCELLED">Cancelled</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>

                        <div className="grid gap-2">
                            <Label htmlFor="startLoc">Start Location Name</Label>
                            <Input id="startLoc" value={tripFormData.startLocationName} onChange={(e) => setTripFormData({ ...tripFormData, startLocationName: e.target.value })} placeholder="e.g. Fort Bus Stand" />
                        </div>

                        <div className="grid gap-2">
                            <Label htmlFor="endLoc">End Location Name</Label>
                            <Input id="endLoc" value={tripFormData.endLocationName} onChange={(e) => setTripFormData({ ...tripFormData, endLocationName: e.target.value })} placeholder="e.g. Kottawa" />
                        </div>

                        <div className="grid grid-cols-3 gap-4">
                            <div className="grid gap-2">
                                <Label htmlFor="distance">Distance (km)</Label>
                                <Input id="distance" type="number" step="0.1" value={tripFormData.distanceKm} onChange={(e) => setTripFormData({ ...tripFormData, distanceKm: Number(e.target.value) })} />
                            </div>
                            <div className="grid gap-2">
                                <Label htmlFor="fare">Total Fare</Label>
                                <Input id="fare" type="number" step="0.01" value={tripFormData.totalFare} onChange={(e) => setTripFormData({ ...tripFormData, totalFare: Number(e.target.value) })} />
                            </div>
                            <div className="grid gap-2">
                                <Label htmlFor="pts">Points Earned</Label>
                                <Input id="pts" type="number" value={tripFormData.pointsEarned} onChange={(e) => setTripFormData({ ...tripFormData, pointsEarned: Number(e.target.value) })} />
                            </div>
                        </div>
                    </div>

                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsTripDialogOpen(false)} disabled={isSubmittingTrip}>Cancel</Button>
                        <Button onClick={handleSaveTrip} disabled={isSubmittingTrip}>
                            {isSubmittingTrip ? "Saving..." : "Save Trip"}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Wallet Adjustment Dialog */}
            <Dialog open={isAdjustingWallet} onOpenChange={setIsAdjustingWallet}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Manual Wallet Adjustment</DialogTitle>
                        <DialogDescription>
                            Credit or debit a user's wallet balance directly. This action will be logged.
                        </DialogDescription>
                    </DialogHeader>

                    <div className="space-y-4 py-4">
                        <div className="space-y-2">
                            <label className="text-sm font-medium">User</label>
                            <Select value={selectedUserId || ""} onValueChange={(val) => setSelectedUserId(val)}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select a user..." />
                                </SelectTrigger>
                                <SelectContent>
                                    {users.map(u => (
                                        <SelectItem key={u.id} value={u.id}>
                                            {u.displayName || u.email || u.id}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-medium">Adjustment Amount (Rs)</label>
                            <div className="relative">
                                <span className="absolute left-3 top-2.5 text-sm text-zinc-500 font-medium">Rs</span>
                                <Input
                                    type="number"
                                    placeholder="e.g. 500 or -200"
                                    className="pl-8"
                                    value={adjustmentAmount}
                                    onChange={(e) => setAdjustmentAmount(e.target.value)}
                                />
                            </div>
                            <p className="text-xs text-zinc-500">Use negative values to deduct balance.</p>
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-medium">Reason for Adjustment</label>
                            <Input
                                placeholder="e.g. Refund for overcharged trip, Promotional credit"
                                value={adjustmentReason}
                                onChange={(e) => setAdjustmentReason(e.target.value)}
                            />
                        </div>
                    </div>

                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsAdjustingWallet(false)} disabled={isSubmittingWallet}>Cancel</Button>
                        <Button onClick={handleAdjustWalletSubmit} disabled={isSubmittingWallet}>
                            {isSubmittingWallet ? "Processing..." : "Confirm Adjustment"}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
