"use client";

/**
 * QRGeneratorPage — Admin module for generating and printing Bus QR Codes.
 *
 * This page allows administrators to select a bus from the fleet and generate
 * a unique QR code sticker. Passengers scan these stickers via the mobile app
 * to check-in, leave feedback, or report lost items.
 *
 * The page utilizes `qrcode.react` to generate SVG QR codes and uses a plain HTML
 * print window to send the sticker to the printer.
 */

import { useState, useEffect, useMemo, useRef, useCallback } from "react";
import { collection, getDocs } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { QRCodeSVG } from "qrcode.react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { QrCode, Printer, RefreshCw, Bus } from "lucide-react";
import { toast } from "sonner";

// ─── Type Definitions ───────────────────────────────────────

/** Structure of a Bus document from Firestore */
interface BusData {
    id: string;
    registrationNumber: string;
    defaultRouteId: string;
    capacity: number;
    type: string;
    qrCodeString?: string;
}

/** Structure of a Route document from Firestore */
interface RouteData {
    id: string;
    routeId: string;
    name?: string;
    startLocationName?: string;
    endLocationName?: string;
}

// ═══════════════════════════════════════════════════════════
// ─── QR GENERATOR COMPONENT ─────────────────────────────
// ═══════════════════════════════════════════════════════════

export default function QRGeneratorPage() {
    // ─── State ──────────────────────────────────────────────
    const [buses, setBuses] = useState<BusData[]>([]);
    const [routes, setRoutes] = useState<RouteData[]>([]);
    const [selectedBus, setSelectedBus] = useState<BusData | null>(null);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);

    /** Reference to the SVG container so we can extract its HTML for printing */
    const qrRef = useRef<HTMLDivElement>(null);

    // ─── Lookup Maps ────────────────────────────────────────

    /**
     * Map of Route IDs to readable labels.
     * e.g., "R-138" => "138 — Maharagama to Pettah"
     */
    const routeMap = useMemo(() => {
        const map: Record<string, string> = {};
        routes.forEach(r => {
            const hasEnds = r.startLocationName && r.endLocationName;
            map[r.id] = hasEnds
                ? `${r.routeId || r.id} — ${r.startLocationName} to ${r.endLocationName}`
                : (r.name || r.routeId || r.id);
        });
        return map;
    }, [routes]);

    // ─── Data Fetching ──────────────────────────────────────

    /**
     * Fetches both buses and routes concurrently from Firestore.
     * Sorts buses alphabetically by registration number for the dropdown.
     */
    const fetchData = useCallback(async (showToast = false) => {
        try {
            if (showToast) setRefreshing(true);

            // Fetch buses and routes in parallel
            const [busSnap, routeSnap] = await Promise.all([
                getDocs(collection(db, "buses")),
                getDocs(collection(db, "routes"))
            ]);

            // Process Buses
            const busList: BusData[] = [];
            busSnap.forEach((doc) => busList.push({ id: doc.id, ...doc.data() } as BusData));
            busList.sort((a, b) => (a.registrationNumber || "").localeCompare(b.registrationNumber || ""));
            setBuses(busList);

            // Process Routes
            const routeList: RouteData[] = [];
            routeSnap.forEach((doc) => routeList.push({ id: doc.id, ...doc.data() } as RouteData));
            setRoutes(routeList);

            if (showToast) toast.success("Data refreshed");
        } catch (e) {
            console.error("Error fetching QR data:", e);
            toast.error("Failed to load buses data.");
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, []);

    // Initial load
    useEffect(() => {
        fetchData();
    }, [fetchData]);

    // ─── Handlers ───────────────────────────────────────────

    /**
     * Opens a new browser window, injects the SVG QR code and bus details,
     * and triggers the browser's native print dialog.
     */
    const handlePrint = () => {
        if (!selectedBus) return;

        // Retrieve the raw `<svg>` HTML from the DOM
        const printContent = qrRef.current?.innerHTML;
        if (!printContent) {
            toast.error("Failed to read QR code for printing.");
            return;
        }

        const routeLabel = routeMap[selectedBus.defaultRouteId] || selectedBus.defaultRouteId || 'Unassigned';

        // Open a blank popup window for printing
        const printWindow = window.open('', '_blank', 'width=600,height=800');
        if (printWindow) {
            printWindow.document.write(`
        <!DOCTYPE html>
        <html>
          <head>
            <title>QR Code - ${selectedBus.registrationNumber}</title>
            <style>
              @page { margin: 0; size: auto; }
              body { 
                font-family: system-ui, -apple-system, sans-serif; 
                display: flex; 
                flex-direction: column; 
                align-items: center; 
                justify-content: center; 
                height: 100vh; 
                margin: 0;
                padding: 20px;
                text-align: center;
              }
              .header { font-size: 28px; font-weight: bold; margin-bottom: 8px; }
              .subheader { font-size: 16px; color: #666; margin-bottom: 32px; }
              .qr-container { padding: 24px; border: 2px dashed #ccc; border-radius: 16px; margin-bottom: 24px; }
              .route { font-size: 18px; font-weight: 500; color: #333; margin-bottom: 8px; }
              .type { font-size: 14px; background: #eee; padding: 4px 12px; border-radius: 100px; display: inline-block; }
              .scan-text { margin-top: 32px; font-size: 14px; color: #888; max-width: 300px; line-height: 1.5; }
            </style>
          </head>
          <body>
            <div class="header">${selectedBus.registrationNumber}</div>
            <div class="subheader">BusLK Official Fleet Vehicle</div>
            
            <div class="qr-container">
              ${printContent}
            </div>
            
            <div class="route">Route: ${routeLabel}</div>
            <div class="type">${selectedBus.type || 'PUBLIC TRANSIT'}</div>
            
            <div class="scan-text">
              Scan this QR code using the BusLK mobile app to check-in, view live updates, or report an issue.
            </div>
            
            <script>
              // Triggers the print dialog as soon as the window loads
              window.onload = function() { window.print(); window.close(); }
            </script>
          </body>
        </html>
      `);
            printWindow.document.close();
        } else {
            toast.error("Popup blocked. Please allow popups to print.");
        }
    };

    /** Handles bus selection from the dropdown */
    const onBusSelect = (busId: string) => {
        const bus = buses.find(b => b.id === busId);
        setSelectedBus(bus || null);
    };

    // ═══════════════════════════════════════════════════════════
    // ─── RENDER ─────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════

    return (
        <div className="space-y-6">
            {/* ─── Page Header ───────────────────────────────── */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center bg-white dark:bg-zinc-950 p-6 rounded-xl border shadow-sm gap-4">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">QR Code Generator</h2>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-1">Generate and print check-in stickers for the active fleet.</p>
                </div>
                <Button variant="outline" size="sm" onClick={() => fetchData(true)} disabled={loading || refreshing}>
                    <RefreshCw className={`h-4 w-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                    Refresh
                </Button>
            </div>

            {/* ─── Main Content Grid ─────────────────────────── */}
            <div className="grid gap-6 md:grid-cols-12">
                {/* Left Column: Selector */}
                <div className="md:col-span-5 space-y-6">
                    <Card className="shadow-sm">
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <Bus className="h-5 w-5 text-blue-600" />
                                Select Vehicle
                            </CardTitle>
                            <CardDescription>
                                Choose a bus to generate its unique identification QR sticker.
                            </CardDescription>
                        </CardHeader>

                        <CardContent className="space-y-6">
                            {/* Bus Dropdown */}
                            <div className="space-y-2">
                                <Label className="text-sm font-semibold text-zinc-700 dark:text-zinc-300">
                                    Bus Registration Number
                                </Label>

                                {loading ? (
                                    <Skeleton className="h-10 w-full rounded-md" />
                                ) : (
                                    <Select
                                        value={selectedBus?.id || ""}
                                        onValueChange={onBusSelect}
                                    >
                                        <SelectTrigger className="bg-zinc-50 dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800">
                                            <SelectValue placeholder="Search or select a bus..." />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {buses.length === 0 && (
                                                <SelectItem value="_none" disabled>No buses found</SelectItem>
                                            )}
                                            {buses.map(bus => (
                                                <SelectItem key={bus.id} value={bus.id}>
                                                    {bus.registrationNumber || bus.id}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                )}
                            </div>

                            {/* Selected Bus Details Card */}
                            {selectedBus && (
                                <div className="p-4 bg-blue-50/50 dark:bg-blue-900/10 rounded-lg space-y-3 border border-blue-100 dark:border-blue-900/30">
                                    <div>
                                        <p className="text-xs font-semibold text-zinc-500 tracking-wider uppercase mb-1">Assigned Route</p>
                                        <p className="text-sm font-medium text-zinc-900 dark:text-zinc-100">
                                            {routeMap[selectedBus.defaultRouteId] || selectedBus.defaultRouteId || 'Unassigned'}
                                        </p>
                                    </div>
                                    <div className="flex items-center gap-4">
                                        <div>
                                            <p className="text-xs font-semibold text-zinc-500 tracking-wider uppercase mb-1">Capacity</p>
                                            <p className="text-sm font-medium text-zinc-900 dark:text-zinc-100">{selectedBus.capacity || 0} pax</p>
                                        </div>
                                        <div>
                                            <p className="text-xs font-semibold text-zinc-500 tracking-wider uppercase mb-1">Fleet Type</p>
                                            <Badge variant="outline" className={selectedBus.type === 'CTB' ? 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950 dark:text-emerald-300 dark:border-emerald-800' : 'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-950 dark:text-blue-300 dark:border-blue-800'}>
                                                {selectedBus.type || 'UNKNOWN'}
                                            </Badge>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </div>

                {/* Right Column: QR Preview and Print */}
                <div className="md:col-span-7">
                    {selectedBus ? (
                        <Card className="flex flex-col items-center justify-center p-8 text-center shadow-sm h-full relative overflow-hidden">
                            {/* Decorative background blob */}
                            <div className="absolute top-0 right-0 w-64 h-64 bg-blue-50 dark:bg-blue-900/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/3 opacity-50 pointer-events-none" />

                            {/* QR Code SVG Container */}
                            <div
                                ref={qrRef}
                                className="p-6 bg-white rounded-2xl shadow-sm border border-zinc-100 dark:border-zinc-800 mb-6 relative z-10"
                            >
                                <QRCodeSVG
                                    // Fallback to a standard format if qrCodeString is missing
                                    value={selectedBus.qrCodeString || `buslk:checkin:${selectedBus.id}`}
                                    size={240}
                                    level="H"         // High error correction (good for public stickers)
                                    includeMargin={true}
                                />
                            </div>

                            <div className="space-y-1 z-10">
                                <h3 className="text-2xl font-bold tracking-tight">{selectedBus.registrationNumber}</h3>
                                {selectedBus.qrCodeString && (
                                    <p className="text-zinc-500 font-mono text-xs">{selectedBus.qrCodeString}</p>
                                )}
                            </div>

                            <Button onClick={handlePrint} size="lg" className="w-full max-w-sm mt-8 z-10 shadow-md">
                                <Printer className="mr-2 h-5 w-5" />
                                Send to Printer
                            </Button>
                        </Card>
                    ) : (
                        <Card className="flex flex-col items-center justify-center p-12 text-center text-zinc-500 dark:text-zinc-400 border-dashed h-full shadow-none bg-zinc-50/50 dark:bg-zinc-900/20">
                            <div className="h-20 w-20 rounded-full bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center mb-6">
                                <QrCode className="h-10 w-10 text-zinc-400" />
                            </div>
                            <h3 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100 mb-2">No Vehicle Selected</h3>
                            <p className="max-w-[250px] text-sm">Select a bus from the list to preview and print its unique QR code sticker.</p>
                        </Card>
                    )}
                </div>
            </div>
        </div >
    );
}
