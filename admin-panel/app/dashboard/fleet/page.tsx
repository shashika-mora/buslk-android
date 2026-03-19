"use client";

/**
 * FleetPage — Admin module for managing the bus fleet.
 *
 * Features:
 * - View all registered buses in a searchable list
 * - Add new buses with registration number, route, owner, type (PRIVATE/CTB), and capacity
 * - Edit existing bus details (registration number used as document ID, so it's immutable)
 * - Delete buses with confirmation
 * - View detailed bus info in a side panel
 * - Route dropdown fetched from Firestore for consistent data linking
 *
 * Schema (db.md): { registrationNumber, owner, type, capacity, qrCodeString, defaultRouteId, lastMaintenance }
 */

import { useEffect, useState, useCallback, useMemo } from "react";
import { collection, getDocs, setDoc, deleteDoc, doc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import { Plus, Trash2, Bus, Pencil, Search, RefreshCcw, X } from "lucide-react";

// ─── Type Definitions ───────────────────────────────────────

/** Represents a bus document in Firestore */
interface BusNode {
    id: string;
    registrationNumber?: string;
    defaultRouteId?: string;
    owner?: string;
    type?: string;
    capacity?: number;
    lastMaintenance?: string;
    qrCodeString?: string;
    [key: string]: any;
}

/** Represents a route document in Firestore */
interface RouteNode {
    id: string;
    routeId?: string;
    name?: string;
    [key: string]: any;
}

/** Form data for creating or editing a bus */
interface BusFormData {
    registrationNumber: string;
    defaultRouteId: string;
    owner: string;
    type: string;        // "PRIVATE" or "CTB"
    capacity: string;    // Stored as string in form, parsed to number on save
}

/** Default form values */
const DEFAULT_FORM: BusFormData = {
    registrationNumber: "",
    defaultRouteId: "",
    owner: "",
    type: "PRIVATE",
    capacity: "54",
};

export default function FleetPage() {
    // ─── State ──────────────────────────────────────────────
    const [buses, setBuses] = useState<BusNode[]>([]);
    const [routes, setRoutes] = useState<RouteNode[]>([]);
    const [searchQuery, setSearchQuery] = useState("");
    const [loading, setLoading] = useState(true);

    // Form state (consolidated into a single object)
    const [formData, setFormData] = useState<BusFormData>({ ...DEFAULT_FORM });
    const [isEditing, setIsEditing] = useState(false);
    const [selectedBus, setSelectedBus] = useState<BusNode | null>(null);

    // ─── Route Lookup Map ───────────────────────────────────
    /** Maps route document ID → display label (e.g. "100 — Colombo → Panadura") */
    const routeMap = useMemo(() => {
        const map: Record<string, string> = {};
        routes.forEach(r => {
            map[r.id] = r.routeId
                ? `${r.routeId} — ${r.name || ""}`
                : r.name || r.id;
        });
        return map;
    }, [routes]);

    // ─── Data Fetching ──────────────────────────────────────

    /** Fetch all buses from Firestore */
    const fetchBuses = useCallback(async () => {
        try {
            setLoading(true);
            const querySnapshot = await getDocs(collection(db, "buses"));
            const busList: BusNode[] = [];
            querySnapshot.forEach((d) => {
                busList.push({ id: d.id, ...d.data() } as BusNode);
            });
            // Sort alphabetically by registration number
            busList.sort((a, b) =>
                (a.registrationNumber || a.id).localeCompare(b.registrationNumber || b.id)
            );
            setBuses(busList);
        } catch (e) {
            console.error("Error fetching buses:", e);
            toast.error("Failed to load fleet data.");
        } finally {
            setLoading(false);
        }
    }, []);

    /** Fetch all routes for the dropdown selector */
    const fetchRoutes = useCallback(async () => {
        try {
            const snap = await getDocs(collection(db, "routes"));
            const list: RouteNode[] = [];
            snap.forEach(d => list.push({ id: d.id, ...d.data() } as RouteNode));
            // Sort by routeId numerically for easier browsing
            list.sort((a, b) => (a.routeId || "").localeCompare(b.routeId || "", undefined, { numeric: true }));
            setRoutes(list);
        } catch (e) {
            console.error("Failed to load routes:", e);
        }
    }, []);

    /** Fetch both buses and routes on component mount */
    useEffect(() => {
        fetchBuses();
        fetchRoutes();
    }, [fetchBuses, fetchRoutes]);

    // ─── Filtered Buses (search) ────────────────────────────

    /** Filter buses based on search query across registration, route, owner, type */
    const filteredBuses = useMemo(() => {
        if (!searchQuery.trim()) return buses;
        const q = searchQuery.toLowerCase();
        return buses.filter(bus =>
            (bus.registrationNumber || bus.id || "").toLowerCase().includes(q) ||
            (bus.defaultRouteId || "").toLowerCase().includes(q) ||
            (bus.defaultRouteId ? (routeMap[bus.defaultRouteId] || "") : "").toLowerCase().includes(q) ||
            (bus.owner || "").toLowerCase().includes(q) ||
            (bus.type || "").toLowerCase().includes(q)
        );
    }, [buses, searchQuery, routeMap]);

    // ─── Form Handlers ──────────────────────────────────────

    /**
     * Populates the form with an existing bus's data for editing.
     * Registration number field is disabled during edit since it's the document ID.
     */
    const handleEditClick = (bus: BusNode) => {
        setFormData({
            registrationNumber: bus.registrationNumber || bus.id,
            defaultRouteId: bus.defaultRouteId || "",
            owner: bus.owner || "",
            type: bus.type || "PRIVATE",
            capacity: bus.capacity?.toString() || "54",
        });
        setIsEditing(true);
    };

    /** Resets the form to default values and exits edit mode */
    const handleCancelEdit = () => {
        setIsEditing(false);
        setFormData({ ...DEFAULT_FORM });
        setSelectedBus(null);
    };

    /**
     * Saves a bus document to Firestore (create or update).
     * Uses `setDoc` with `merge: true` so existing fields (like lastMaintenance) are preserved.
     * The registration number is used as the document ID for easy lookup.
     */
    const handleSaveBus = async (e: React.FormEvent) => {
        e.preventDefault();

        // Validate required fields
        if (!formData.registrationNumber.trim()) {
            toast.error("Registration number is required.");
            return;
        }
        if (!formData.defaultRouteId) {
            toast.error("Please select a default route.");
            return;
        }

        try {
            const busData: Partial<BusNode> = {
                registrationNumber: formData.registrationNumber.trim(),
                defaultRouteId: formData.defaultRouteId,
                owner: formData.owner.trim() || "Private Operator",
                type: formData.type,
                capacity: parseInt(formData.capacity) || 54,
                qrCodeString: `buslk:checkin:${formData.registrationNumber.trim()}`,
            };

            // Only set lastMaintenance for new buses (don't overwrite existing value on edit)
            if (!isEditing) {
                busData.lastMaintenance = new Date().toISOString();
            }

            // Use registration number as document ID for consistent referencing
            await setDoc(
                doc(db, "buses", formData.registrationNumber.trim()),
                busData,
                { merge: true }
            );

            toast.success(isEditing ? "Bus updated successfully." : "Bus added successfully.");
            handleCancelEdit();
            fetchBuses();
        } catch (e: any) {
            console.error("Error saving bus:", e);
            toast.error(isEditing ? "Error updating bus." : "Error adding bus.", { description: e.message });
        }
    };

    /**
     * Deletes a bus document after user confirmation.
     * Also clears the selected bus if it was the one deleted.
     * @param id - Firestore document ID (registration number)
     */
    const handleDeleteBus = async (id: string) => {
        if (!confirm(`Are you sure you want to delete bus "${id}"? This action cannot be undone.`)) return;

        try {
            await deleteDoc(doc(db, "buses", id));
            toast.success("Bus deleted.");
            // Clear selection if the deleted bus was selected
            if (selectedBus?.id === id) setSelectedBus(null);
            fetchBuses();
        } catch (e: any) {
            console.error("Error deleting bus:", e);
            toast.error("Error deleting bus.", { description: e.message });
        }
    };

    // ═══════════════════════════════════════════════════════════
    // ─── RENDER ─────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════

    return (
        <div className="space-y-6">
            {/* ─── Page Header ───────────────────────────────── */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">Fleet Management</h2>
                    <p className="text-muted-foreground mt-1">Register, edit, and manage transit vehicles.</p>
                </div>
                <Button variant="outline" size="sm" onClick={fetchBuses} disabled={loading}>
                    <RefreshCcw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                    Refresh
                </Button>
            </div>

            <div className="grid gap-6 md:grid-cols-2">
                {/* ═══════════════════════════════════════════════ */}
                {/* ─── LEFT COLUMN: Add/Edit Form + Details ── */}
                {/* ═══════════════════════════════════════════════ */}
                <div className="space-y-6">
                    {/* ─── Add / Edit Form Card ────────────────── */}
                    <Card>
                        <CardHeader>
                            <CardTitle>{isEditing ? "Edit Bus Details" : "Add New Bus"}</CardTitle>
                            <CardDescription>
                                {isEditing
                                    ? "Update the details of the selected vehicle."
                                    : "Register a new vehicle into the transit network."}
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            <form onSubmit={handleSaveBus} className="space-y-4">
                                {/* Registration Number (immutable during edit) */}
                                <div className="space-y-2">
                                    <Label>Registration Number</Label>
                                    <Input
                                        placeholder="e.g. NA-1234"
                                        value={formData.registrationNumber}
                                        onChange={(e) => setFormData({ ...formData, registrationNumber: e.target.value.toUpperCase() })}
                                        required
                                        disabled={isEditing}
                                    />
                                    {isEditing && (
                                        <p className="text-xs text-zinc-500">Registration number cannot be changed (used as document ID).</p>
                                    )}
                                </div>

                                {/* Default Route dropdown */}
                                <div className="space-y-2">
                                    <Label>Default Route</Label>
                                    <Select
                                        value={formData.defaultRouteId}
                                        onValueChange={(v) => setFormData({ ...formData, defaultRouteId: v })}
                                    >
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select a route..." />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {routes.length === 0 && (
                                                <SelectItem value="_none" disabled>No routes in database</SelectItem>
                                            )}
                                            {routes.map(r => (
                                                <SelectItem key={r.id} value={r.id}>
                                                    {r.routeId || r.id} — {r.name || ""}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>

                                {/* Owner */}
                                <div className="space-y-2">
                                    <Label>Owner</Label>
                                    <Input
                                        placeholder="e.g. Private Operator"
                                        value={formData.owner}
                                        onChange={(e) => setFormData({ ...formData, owner: e.target.value })}
                                    />
                                </div>

                                {/* Type and Capacity (side by side) */}
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <Label>Type</Label>
                                        <Select
                                            value={formData.type}
                                            onValueChange={(v) => setFormData({ ...formData, type: v })}
                                        >
                                            <SelectTrigger>
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem value="PRIVATE">PRIVATE</SelectItem>
                                                <SelectItem value="CTB">CTB</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </div>
                                    <div className="space-y-2">
                                        <Label>Capacity</Label>
                                        <Input
                                            type="number"
                                            min={1}
                                            placeholder="e.g. 54"
                                            value={formData.capacity}
                                            onChange={(e) => setFormData({ ...formData, capacity: e.target.value })}
                                        />
                                    </div>
                                </div>

                                {/* Submit / Cancel buttons */}
                                <div className="flex space-x-2">
                                    <Button type="submit" className="flex-1">
                                        {isEditing ? (
                                            <>
                                                <Pencil className="mr-2 h-4 w-4" />
                                                Update Vehicle
                                            </>
                                        ) : (
                                            <>
                                                <Plus className="mr-2 h-4 w-4" />
                                                Add Vehicle
                                            </>
                                        )}
                                    </Button>
                                    {isEditing && (
                                        <Button type="button" variant="outline" onClick={handleCancelEdit}>
                                            <X className="mr-2 h-4 w-4" />
                                            Cancel
                                        </Button>
                                    )}
                                </div>
                            </form>
                        </CardContent>
                    </Card>

                    {/* ─── Bus Details Side Panel ─────────────── */}
                    {selectedBus && (
                        <Card className="animate-in fade-in slide-in-from-top-2">
                            <CardHeader className="flex flex-row items-center justify-between pb-2">
                                <div className="space-y-1">
                                    <CardTitle>Bus Details</CardTitle>
                                    <CardDescription>Information for {selectedBus.registrationNumber || selectedBus.id}</CardDescription>
                                </div>
                                <div className="space-x-2">
                                    <Button variant="outline" size="sm" onClick={() => handleEditClick(selectedBus)}>
                                        <Pencil className="mr-2 h-4 w-4" /> Edit
                                    </Button>
                                    <Button variant="outline" size="sm" onClick={() => setSelectedBus(null)}>
                                        <X className="mr-2 h-4 w-4" /> Close
                                    </Button>
                                </div>
                            </CardHeader>
                            <CardContent className="space-y-3 text-sm">
                                <div className="grid grid-cols-2 gap-2">
                                    <span className="font-semibold text-zinc-500">Registration:</span>
                                    <span>{selectedBus.registrationNumber || selectedBus.id}</span>

                                    <span className="font-semibold text-zinc-500">Route:</span>
                                    <span>{selectedBus.defaultRouteId && routeMap[selectedBus.defaultRouteId] ? routeMap[selectedBus.defaultRouteId] : (selectedBus.defaultRouteId || "N/A")}</span>

                                    <span className="font-semibold text-zinc-500">Owner:</span>
                                    <span>{selectedBus.owner || "N/A"}</span>

                                    <span className="font-semibold text-zinc-500">Type:</span>
                                    <span>
                                        <Badge variant={selectedBus.type === "CTB" ? "default" : "secondary"}>
                                            {selectedBus.type || "N/A"}
                                        </Badge>
                                    </span>

                                    <span className="font-semibold text-zinc-500">Capacity:</span>
                                    <span>{selectedBus.capacity || "N/A"} seats</span>

                                    <span className="font-semibold text-zinc-500">Last Maintenance:</span>
                                    <span>
                                        {selectedBus.lastMaintenance
                                            ? new Date(selectedBus.lastMaintenance).toLocaleDateString()
                                            : "N/A"}
                                    </span>

                                    <span className="font-semibold text-zinc-500 col-span-2 mt-2">QR String:</span>
                                    <span className="truncate col-span-2 font-mono bg-zinc-100 dark:bg-zinc-800 p-2 rounded text-xs">
                                        {selectedBus.qrCodeString || "N/A"}
                                    </span>
                                </div>
                            </CardContent>
                        </Card>
                    )}
                </div>

                {/* ═══════════════════════════════════════════════ */}
                {/* ─── RIGHT COLUMN: Active Fleet List ───────── */}
                {/* ═══════════════════════════════════════════════ */}
                <Card>
                    <CardHeader>
                        <CardTitle>Active Fleet</CardTitle>
                        <CardDescription>
                            {buses.length > 0
                                ? `${buses.length} vehicles registered`
                                : "Manage currently registered buses."}
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        {/* Search bar */}
                        <div className="relative mb-4">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-500" />
                            <Input
                                placeholder="Search by reg. number, route, owner, type..."
                                className="pl-10"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                            />
                        </div>

                        {/* Results count */}
                        {!loading && buses.length > 0 && (
                            <p className="text-xs text-zinc-500 mb-3">
                                Showing {filteredBuses.length} of {buses.length} buses
                                {searchQuery && ` matching "${searchQuery}"`}
                            </p>
                        )}

                        {loading ? (
                            /* Loading skeleton */
                            <div className="space-y-3">
                                {[1, 2, 3, 4].map(i => (
                                    <div key={i} className="animate-pulse flex items-center justify-between p-4 border rounded-lg">
                                        <div className="flex items-center space-x-4">
                                            <div className="h-9 w-9 bg-zinc-200 dark:bg-zinc-700 rounded-full" />
                                            <div className="space-y-2">
                                                <div className="h-4 w-24 bg-zinc-200 dark:bg-zinc-700 rounded" />
                                                <div className="h-3 w-32 bg-zinc-100 dark:bg-zinc-800 rounded" />
                                            </div>
                                        </div>
                                        <div className="h-8 w-8 bg-zinc-100 dark:bg-zinc-800 rounded" />
                                    </div>
                                ))}
                            </div>
                        ) : filteredBuses.length === 0 ? (
                            /* Empty state */
                            <div className="text-center py-12 text-zinc-500 bg-zinc-50 dark:bg-zinc-900/50 rounded-lg">
                                <Bus className="h-8 w-8 opacity-20 mx-auto mb-3" />
                                <p className="font-medium">
                                    {searchQuery ? "No matching buses" : "No buses registered"}
                                </p>
                                <p className="text-xs mt-1">
                                    {searchQuery
                                        ? `No results for "${searchQuery}".`
                                        : "Use the form to register your first vehicle."}
                                </p>
                            </div>
                        ) : (
                            /* Bus list */
                            <div className="space-y-3">
                                {filteredBuses.map((bus) => (
                                    <div
                                        key={bus.id}
                                        className={`flex items-center justify-between p-4 border rounded-lg transition-colors cursor-pointer ${selectedBus?.id === bus.id
                                            ? "bg-zinc-100 border-zinc-300 dark:bg-zinc-800 dark:border-zinc-700"
                                            : "hover:bg-zinc-50 dark:border-zinc-800 dark:hover:bg-zinc-900"
                                            }`}
                                        onClick={() => setSelectedBus(bus)}
                                    >
                                        {/* Bus icon + info */}
                                        <div className="flex items-center space-x-4">
                                            <div className={`p-2 rounded-full ${bus.type === "CTB" ? "bg-green-100 dark:bg-green-900/30" : "bg-blue-100 dark:bg-blue-900/30"}`}>
                                                <Bus className={`h-5 w-5 ${bus.type === "CTB" ? "text-green-600 dark:text-green-400" : "text-blue-600 dark:text-blue-400"}`} />
                                            </div>
                                            <div>
                                                <div className="flex items-center gap-2">
                                                    <p className="font-semibold">{bus.registrationNumber || bus.id}</p>
                                                    <Badge variant={bus.type === "CTB" ? "default" : "outline"} className="text-[10px] px-1.5 py-0">
                                                        {bus.type || "N/A"}
                                                    </Badge>
                                                </div>
                                                <p className="text-sm text-zinc-500">
                                                    {bus.defaultRouteId && routeMap[bus.defaultRouteId] ? routeMap[bus.defaultRouteId] : (bus.defaultRouteId || "No route assigned")}
                                                </p>
                                            </div>
                                        </div>

                                        {/* Delete button (stops click propagation to avoid selecting) */}
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30 z-10"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleDeleteBus(bus.id);
                                            }}
                                        >
                                            <Trash2 className="h-4 w-4" />
                                        </Button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}
