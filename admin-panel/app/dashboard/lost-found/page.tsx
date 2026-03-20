"use client";

/**
 * LostFoundPage — Admin module for managing lost and found reports.
 *
 * Features:
 * - Tabbed view for LOST vs FOUND items
 * - Create, edit, and delete reports with Firestore integration
 * - Filter by status (OPEN / RESOLVED / CLAIMED) and free-text search
 * - Route and bus dropdowns populated from Firestore (bus cascades from route)
 * - User dropdown for selecting reporters from the `users` collection
 * - Inline status change directly from item cards
 * - Skeleton loading, results count, and Refresh button
 *
 * Firestore Collection: `lost_found`
 * Schema: { itemType, title, description, busId, routeId, userId, status, timestamp, imageUrl? }
 */

import { useEffect, useState, useCallback, useMemo } from "react";
import { collection, getDocs, addDoc, deleteDoc, doc, updateDoc, serverTimestamp } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Search, MapPin, Bus, Calendar, User, PackageSearch, Image as ImageIcon, Plus, Trash2, Edit2, RefreshCcw } from "lucide-react";
import { toast } from "sonner";
import { format } from "date-fns";

// ─── Type Definitions ───────────────────────────────────────

/** Represents a lost/found document in Firestore */
interface LostFoundItem {
    id: string;
    itemType?: string;
    title?: string;
    description?: string;
    busId?: string;
    routeId?: string;
    userId?: string;
    status?: string;
    timestamp?: any;
    imageUrl?: string;
    [key: string]: any;
}

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

/** Represents a user document in Firestore */
interface UserNode {
    id: string;
    displayName?: string;
    email?: string;
    [key: string]: any;
}

/** Form data for creating or editing a lost/found report */
interface LostFoundFormData {
    itemType: string;       // "LOST" or "FOUND"
    title: string;
    description: string;
    busId: string;          // FK → buses
    routeId: string;        // FK → routes
    userId: string;         // FK → users (reporter)
    status: string;         // "OPEN", "RESOLVED", or "CLAIMED"
}

/** Default (empty) form state for new reports */
const DEFAULT_FORM: LostFoundFormData = {
    itemType: "LOST",
    title: "",
    description: "",
    busId: "",
    routeId: "",
    userId: "",
    status: "OPEN",
};

export default function LostFoundPage() {
    // ─── State ──────────────────────────────────────────────
    const [items, setItems] = useState<LostFoundItem[]>([]);
    const [routes, setRoutes] = useState<RouteNode[]>([]);
    const [buses, setBuses] = useState<BusNode[]>([]);
    const [users, setUsers] = useState<UserNode[]>([]);
    const [loading, setLoading] = useState(true);

    // Filter state
    const [activeTab, setActiveTab] = useState("LOST");
    const [searchQuery, setSearchQuery] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");

    // Dialog state
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [editingId, setEditingId] = useState<string | null>(null);
    const [formData, setFormData] = useState<LostFoundFormData>({ ...DEFAULT_FORM });

    // ─── Lookup Maps ────────────────────────────────────────

    /** Maps route document ID → display label */
    const routeMap = useMemo(() => {
        const map: Record<string, string> = {};
        routes.forEach(r => {
            map[r.id] = r.routeId
                ? `${r.routeId} — ${r.name || ""}`
                : r.name || r.id;
        });
        return map;
    }, [routes]);

    /** Maps bus document ID → registration number */
    const busMap = useMemo(() => {
        const map: Record<string, string> = {};
        buses.forEach(b => {
            map[b.id] = b.registrationNumber || b.id;
        });
        return map;
    }, [buses]);

    /** Maps user UID → display name */
    const userMap = useMemo(() => {
        const map: Record<string, string> = {};
        users.forEach(u => {
            map[u.id] = u.displayName || u.email || u.id;
        });
        return map;
    }, [users]);

    // ─── Data Fetching ──────────────────────────────────────

    /** Fetches all lost_found items from Firestore, sorted by timestamp descending */
    const fetchItems = useCallback(async () => {
        try {
            setLoading(true);
            const querySnapshot = await getDocs(collection(db, "lost_found"));

            const fetchedItems: LostFoundItem[] = [];
            querySnapshot.forEach((d) => {
                fetchedItems.push({ id: d.id, ...d.data() } as LostFoundItem);
            });

            // Sort by timestamp descending (newest first)
            fetchedItems.sort((a, b) => {
                const timeA = a.timestamp?.toDate ? a.timestamp.toDate().getTime() : (a.timestamp ? new Date(a.timestamp).getTime() : 0);
                const timeB = b.timestamp?.toDate ? b.timestamp.toDate().getTime() : (b.timestamp ? new Date(b.timestamp).getTime() : 0);
                return timeB - timeA;
            });

            setItems(fetchedItems);
        } catch (error) {
            console.error("Error fetching lost & found items:", error);
            toast.error("Failed to load Lost & Found records.");
        } finally {
            setLoading(false);
        }
    }, []);

    /** Fetches routes, buses, and users for dropdown population */
    const fetchRelatedData = useCallback(async () => {
        try {
            // Fetch routes
            const routeSnap = await getDocs(collection(db, "routes"));
            const routeList: RouteNode[] = [];
            routeSnap.forEach(d => routeList.push({ id: d.id, ...d.data() } as RouteNode));
            routeList.sort((a, b) => (a.routeId || "").localeCompare(b.routeId || "", undefined, { numeric: true }));
            setRoutes(routeList);

            // Fetch buses
            const busSnap = await getDocs(collection(db, "buses"));
            const busList: BusNode[] = [];
            busSnap.forEach(d => busList.push({ id: d.id, ...d.data() } as BusNode));
            busList.sort((a, b) => (a.registrationNumber || a.id).localeCompare(b.registrationNumber || b.id));
            setBuses(busList);

            // Fetch users for the reporter dropdown
            const userSnap = await getDocs(collection(db, "users"));
            const userList: UserNode[] = [];
            userSnap.forEach(d => userList.push({ id: d.id, ...d.data() } as UserNode));
            userList.sort((a, b) => (a.displayName || "").localeCompare(b.displayName || ""));
            setUsers(userList);
        } catch (error) {
            console.error("Error fetching related data:", error);
        }
    }, []);

    /** Fetch everything on component mount */
    useEffect(() => {
        fetchItems();
        fetchRelatedData();
    }, [fetchItems, fetchRelatedData]);

    // ─── Filtered Items (search + status + tab) ─────────────

    /** Items filtered by active tab, status filter, and search query */
    const filteredItems = useMemo(() => {
        return items.filter(item => {
            // Tab filter: LOST or FOUND
            if (item.itemType !== activeTab) return false;

            // Status filter
            if (statusFilter !== "ALL" && item.status !== statusFilter) return false;

            // Text search across multiple fields
            if (searchQuery.trim()) {
                const q = searchQuery.toLowerCase();
                const fields = [
                    item.title,
                    item.description,
                    item.busId,
                    item.busId ? busMap[item.busId] : undefined,
                    item.routeId,
                    item.routeId ? routeMap[item.routeId] : undefined,
                    item.userId ? userMap[item.userId] : undefined,
                ].filter(Boolean);

                if (!fields.some(f => f!.toLowerCase().includes(q))) return false;
            }

            return true;
        });
    }, [items, activeTab, statusFilter, searchQuery, busMap, routeMap, userMap]);

    /** Count of LOST / FOUND items for tab badges */
    const lostCount = useMemo(() => items.filter(i => i.itemType === "LOST").length, [items]);
    const foundCount = useMemo(() => items.filter(i => i.itemType === "FOUND").length, [items]);

    // ─── Handlers ───────────────────────────────────────────

    /**
     * Updates the status of an item directly (inline status change).
     * Uses optimistic local state update for instant UI feedback.
     */
    const handleStatusChange = async (itemId: string, newStatus: string) => {
        try {
            await updateDoc(doc(db, "lost_found", itemId), { status: newStatus });

            // Optimistic update: mutate local state without re-fetching
            setItems(prev => prev.map(item =>
                item.id === itemId ? { ...item, status: newStatus } : item
            ));

            toast.success(`Status updated to ${newStatus}.`);
        } catch (error) {
            console.error("Error updating status:", error);
            toast.error("Failed to update status.");
        }
    };

    /**
     * Opens the create/edit dialog.
     * If `item` is provided, pre-fills the form for editing.
     * If `typeOverride` is provided (e.g. "LOST"), sets the type for new items.
     */
    const handleOpenDialog = (item?: LostFoundItem, typeOverride?: string) => {
        if (item) {
            // Edit mode: populate form with existing data
            setEditingId(item.id);
            setFormData({
                itemType: item.itemType || "LOST",
                title: item.title || "",
                description: item.description || "",
                busId: item.busId || "",
                routeId: item.routeId || "",
                userId: item.userId || "",
                status: item.status || "OPEN",
            });
        } else {
            // Create mode: reset form to defaults
            setEditingId(null);
            setFormData({
                ...DEFAULT_FORM,
                itemType: typeOverride || activeTab,
            });
        }
        setIsDialogOpen(true);
    };

    /**
     * Saves a lost/found record to Firestore.
     * Uses `addDoc` for new records (auto-generated ID) and `updateDoc` for edits.
     */
    const handleSaveItem = async () => {
        if (!formData.title.trim()) {
            toast.error("Title is required.");
            return;
        }
        if (!formData.description.trim()) {
            toast.error("Description is required.");
            return;
        }

        try {
            setIsSubmitting(true);

            const itemData = {
                itemType: formData.itemType,
                title: formData.title.trim(),
                description: formData.description.trim(),
                busId: formData.busId,
                routeId: formData.routeId,
                userId: formData.userId,
                status: formData.status,
                timestamp: serverTimestamp(),
            };

            if (editingId) {
                // Update existing document
                await updateDoc(doc(db, "lost_found", editingId), itemData);
                toast.success("Record updated successfully.");
            } else {
                // Create new document with auto-generated ID
                await addDoc(collection(db, "lost_found"), itemData);
                toast.success("New record added.");
            }

            setIsDialogOpen(false);
            fetchItems();
        } catch (error) {
            console.error("Error saving record:", error);
            toast.error("Failed to save record.");
        } finally {
            setIsSubmitting(false);
        }
    };

    /**
     * Deletes a lost/found record after user confirmation.
     * @param id - Firestore document ID
     */
    const handleDeleteItem = async (id: string) => {
        if (!confirm("Are you sure you want to delete this record? This action cannot be undone.")) return;

        try {
            await deleteDoc(doc(db, "lost_found", id));
            toast.success("Record deleted.");
            fetchItems();
        } catch (error) {
            console.error("Error deleting record:", error);
            toast.error("Failed to delete record.");
        }
    };

    // ─── Helpers ────────────────────────────────────────────

    /** Returns a Badge variant based on item status */
    const getStatusVariant = (status: string): "destructive" | "default" | "secondary" | "outline" => {
        switch (status) {
            case "OPEN": return "destructive";
            case "RESOLVED": return "default";
            case "CLAIMED": return "secondary";
            default: return "outline";
        }
    };

    /**
     * Formats a Firestore timestamp (or ISO string) to a readable date.
     * Handles both Firestore Timestamp objects (with `.toDate()`) and ISO strings.
     */
    const formatItemDate = (dateVal: any): string => {
        if (!dateVal) return "N/A";
        try {
            // Firestore Timestamp object
            if (typeof dateVal.toDate === "function") {
                return format(dateVal.toDate(), "MMM d, h:mm a");
            }
            // ISO string or other parseable date
            if (!isNaN(new Date(dateVal).getTime())) {
                return format(new Date(dateVal), "MMM d, h:mm a");
            }
            return "Invalid Date";
        } catch {
            return "N/A";
        }
    };

    /** Buses filtered by the currently selected route (for cascading dropdown) */
    const filteredBusesForRoute = useMemo(() => {
        if (!formData.routeId) return buses;
        return buses.filter(b => b.defaultRouteId === formData.routeId);
    }, [buses, formData.routeId]);

    // ═══════════════════════════════════════════════════════════
    // ─── RENDER ─────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════

    return (
        <div className="space-y-6">
            {/* ─── Page Header ───────────────────────────────── */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">Lost &amp; Found</h2>
                    <p className="text-muted-foreground mt-1">Manage network-wide lost and found reports.</p>
                </div>
                <div className="flex space-x-2">
                    <Button variant="outline" size="sm" onClick={fetchItems} disabled={loading}>
                        <RefreshCcw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                        Refresh
                    </Button>
                    <Button
                        onClick={() => handleOpenDialog(undefined, "LOST")}
                        variant="outline"
                        className="border-red-200 text-red-600 hover:bg-red-50 dark:border-red-900/50 dark:hover:bg-red-900/20"
                    >
                        <Plus className="h-4 w-4 mr-2" /> Report Lost
                    </Button>
                    <Button onClick={() => handleOpenDialog(undefined, "FOUND")} className="bg-blue-600 hover:bg-blue-700">
                        <Plus className="h-4 w-4 mr-2" /> Report Found
                    </Button>
                </div>
            </div>

            {/* ─── Search & Status Filter ────────────────────── */}
            <div className="flex flex-col sm:flex-row gap-4">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-500" />
                    <Input
                        placeholder="Search by title, description, bus, route, reporter..."
                        className="pl-10"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>
                <Select value={statusFilter} onValueChange={setStatusFilter}>
                    <SelectTrigger className="w-full sm:w-[180px]">
                        <SelectValue placeholder="Filter by Status" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="ALL">All Statuses</SelectItem>
                        <SelectItem value="OPEN">Open</SelectItem>
                        <SelectItem value="RESOLVED">Resolved</SelectItem>
                        <SelectItem value="CLAIMED">Claimed</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            {/* ─── Tabs: Lost / Found ────────────────────────── */}
            <Tabs defaultValue="LOST" className="w-full" onValueChange={setActiveTab}>
                <TabsList className="grid w-full grid-cols-2 mb-6">
                    <TabsTrigger value="LOST">Reported Lost ({lostCount})</TabsTrigger>
                    <TabsTrigger value="FOUND">Reported Found ({foundCount})</TabsTrigger>
                </TabsList>

                {["LOST", "FOUND"].map((tabContext) => (
                    <TabsContent value={tabContext} key={tabContext} className="space-y-4">
                        {/* Results count */}
                        {!loading && items.length > 0 && (
                            <p className="text-xs text-zinc-500">
                                Showing {filteredItems.length} of {tabContext === "LOST" ? lostCount : foundCount} {tabContext.toLowerCase()} items
                                {searchQuery && ` matching "${searchQuery}"`}
                                {statusFilter !== "ALL" && ` • Status: ${statusFilter}`}
                            </p>
                        )}

                        {loading ? (
                            /* ─── Loading Skeleton ─────────────────── */
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                {[1, 2, 3].map(i => (
                                    <Card key={i} className="animate-pulse">
                                        <div className="w-full h-24 bg-zinc-100 dark:bg-zinc-900 rounded-t-lg" />
                                        <CardHeader className="pb-3">
                                            <div className="flex justify-between">
                                                <div className="h-5 w-32 bg-zinc-200 dark:bg-zinc-700 rounded" />
                                                <div className="h-5 w-16 bg-zinc-200 dark:bg-zinc-700 rounded-full" />
                                            </div>
                                            <div className="h-4 w-full bg-zinc-100 dark:bg-zinc-800 rounded mt-2" />
                                        </CardHeader>
                                        <CardContent className="space-y-2">
                                            <div className="h-3 w-24 bg-zinc-100 dark:bg-zinc-800 rounded" />
                                            <div className="h-3 w-20 bg-zinc-100 dark:bg-zinc-800 rounded" />
                                            <div className="h-3 w-28 bg-zinc-100 dark:bg-zinc-800 rounded" />
                                        </CardContent>
                                    </Card>
                                ))}
                            </div>
                        ) : filteredItems.length === 0 ? (
                            /* ─── Empty State ──────────────────────── */
                            <div className="flex flex-col items-center justify-center py-16 text-center text-zinc-500 bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 rounded-lg">
                                <PackageSearch className="h-12 w-12 mb-4 opacity-20" />
                                <p className="text-lg font-medium">
                                    {searchQuery || statusFilter !== "ALL"
                                        ? "No matching records"
                                        : `No ${tabContext.toLowerCase()} items reported`}
                                </p>
                                <p className="text-sm mt-1">
                                    {searchQuery || statusFilter !== "ALL"
                                        ? "Try adjusting your search or filter."
                                        : `Use the "Report ${tabContext === "LOST" ? "Lost" : "Found"}" button to add one.`}
                                </p>
                            </div>
                        ) : (
                            /* ─── Item Cards Grid ──────────────────── */
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                {filteredItems.map((item) => (
                                    <Card key={item.id} className="flex flex-col group">
                                        {/* Image placeholder / thumbnail */}
                                        {item.imageUrl ? (
                                            <div className="w-full h-48 bg-zinc-100 dark:bg-zinc-900 overflow-hidden rounded-t-lg">
                                                {/* eslint-disable-next-line @next/next/no-img-element */}
                                                <img
                                                    src={item.imageUrl}
                                                    alt={item.title || "Item"}
                                                    className="w-full h-full object-cover"
                                                />
                                            </div>
                                        ) : (
                                            <div className="w-full h-24 bg-zinc-100 dark:bg-zinc-900 flex items-center justify-center rounded-t-lg">
                                                <ImageIcon className="h-8 w-8 text-zinc-300 dark:text-zinc-700" />
                                            </div>
                                        )}

                                        {/* ─── Card Header: title, status, actions ── */}
                                        <CardHeader className="pb-3 relative">
                                            <div className="flex justify-between items-start mb-1 mt-2">
                                                <CardTitle className="text-lg line-clamp-1">{item.title || "Untitled Item"}</CardTitle>
                                                <Badge variant={getStatusVariant(item.status || "OPEN")}>
                                                    {item.status || "OPEN"}
                                                </Badge>
                                            </div>
                                            <CardDescription className="line-clamp-2 min-h-[40px]">
                                                {item.description || "No description provided."}
                                            </CardDescription>
                                        </CardHeader>

                                        {/* ─── Card Content: metadata ──────────────── */}
                                        <CardContent className="space-y-3 pb-4 flex-1">
                                            <div className="grid grid-cols-2 gap-2 text-sm">
                                                {/* Bus info — resolved to registration number */}
                                                <div className="flex items-center text-zinc-500">
                                                    <Bus className="h-3.5 w-3.5 mr-2 flex-shrink-0" />
                                                    <span className="truncate">{(item.busId ? busMap[item.busId] : "") || item.busId || "Unknown"}</span>
                                                </div>
                                                {/* Route info — resolved to display label */}
                                                <div className="flex items-center text-zinc-500">
                                                    <MapPin className="h-3.5 w-3.5 mr-2 flex-shrink-0" />
                                                    <span className="truncate">{(item.routeId ? routeMap[item.routeId] : "") || (item.routeId ? `Route ${item.routeId}` : "N/A")}</span>
                                                </div>
                                                {/* Timestamp */}
                                                <div className="flex items-center text-zinc-500 col-span-2">
                                                    <Calendar className="h-3.5 w-3.5 mr-2 flex-shrink-0" />
                                                    <span className="truncate">{formatItemDate(item.timestamp)}</span>
                                                </div>
                                                {/* Reporter — resolved to display name */}
                                                <div className="flex items-center text-zinc-500 col-span-2">
                                                    <User className="h-3.5 w-3.5 mr-2 flex-shrink-0" />
                                                    <span className="truncate" title={item.userId}>
                                                        Reporter: {(item.userId ? userMap[item.userId] : "") || item.userId || "Unknown"}
                                                    </span>
                                                </div>
                                            </div>

                                            {/* ─── Inline Status Change ────────────── */}
                                            <div className="flex gap-2 mt-4 pt-4 border-t border-zinc-100 dark:border-zinc-800">
                                                <Select
                                                    value={item.status || "OPEN"}
                                                    onValueChange={(val) => handleStatusChange(item.id, val)}
                                                >
                                                    <SelectTrigger className="flex-1 h-8 text-xs">
                                                        <SelectValue />
                                                    </SelectTrigger>
                                                    <SelectContent>
                                                        <SelectItem value="OPEN">Open</SelectItem>
                                                        <SelectItem value="RESOLVED">Resolved</SelectItem>
                                                        <SelectItem value="CLAIMED">Claimed</SelectItem>
                                                    </SelectContent>
                                                </Select>
                                                <Button variant="outline" size="sm" className="h-8 text-xs" onClick={() => handleOpenDialog(item)}>
                                                    <Edit2 className="h-3 w-3 mr-1" /> Edit
                                                </Button>
                                                <Button variant="outline" size="sm" className="h-8 text-xs text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30" onClick={() => handleDeleteItem(item.id)}>
                                                    <Trash2 className="h-3 w-3 mr-1" /> Delete
                                                </Button>
                                            </div>
                                        </CardContent>
                                    </Card>
                                ))}
                            </div>
                        )}
                    </TabsContent>
                ))}
            </Tabs>

            {/* ═══════════════════════════════════════════════════ */}
            {/* ─── Create / Edit Dialog ──────────────────────── */}
            {/* ═══════════════════════════════════════════════════ */}
            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
                <DialogContent className="sm:max-w-[480px]">
                    <DialogHeader>
                        <DialogTitle>
                            {editingId ? "Edit Record" : `Report ${formData.itemType === "LOST" ? "Lost" : "Found"} Item`}
                        </DialogTitle>
                        <DialogDescription>
                            {editingId ? "Update the details of this report." : "Enter details about the item below."}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="grid gap-4 py-4">
                        {/* Type selector (LOST / FOUND) */}
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label className="text-right">Type *</Label>
                            <Select value={formData.itemType} onValueChange={(val) => setFormData({ ...formData, itemType: val })}>
                                <SelectTrigger className="col-span-3">
                                    <SelectValue placeholder="Select type" />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="LOST">Lost</SelectItem>
                                    <SelectItem value="FOUND">Found</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        {/* Title */}
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label className="text-right">Title *</Label>
                            <Input
                                value={formData.title}
                                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                                className="col-span-3"
                                placeholder="e.g. Black Umbrella"
                            />
                        </div>

                        {/* Description */}
                        <div className="grid grid-cols-4 items-start gap-4">
                            <Label className="text-right mt-2">Description *</Label>
                            <Textarea
                                value={formData.description}
                                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                className="col-span-3"
                                placeholder="Details about the item..."
                                rows={3}
                            />
                        </div>

                        {/* Route dropdown (populated from Firestore) */}
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label className="text-right">Route</Label>
                            <Select
                                value={formData.routeId}
                                onValueChange={(val) => setFormData({ ...formData, routeId: val, busId: "" })}
                            >
                                <SelectTrigger className="col-span-3">
                                    <SelectValue placeholder="Select a route..." />
                                </SelectTrigger>
                                <SelectContent>
                                    {routes.length === 0 && <SelectItem value="_none" disabled>No routes found</SelectItem>}
                                    {routes.map(r => (
                                        <SelectItem key={r.id} value={r.id}>
                                            {r.routeId || r.id} — {r.name || ""}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        {/* Bus dropdown (cascading: filtered by selected route) */}
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label className="text-right">Bus</Label>
                            <Select
                                value={formData.busId}
                                onValueChange={(val) => setFormData({ ...formData, busId: val })}
                                disabled={!formData.routeId}
                            >
                                <SelectTrigger className="col-span-3">
                                    <SelectValue placeholder={formData.routeId ? "Select a bus..." : "Select a route first"} />
                                </SelectTrigger>
                                <SelectContent>
                                    {filteredBusesForRoute.length === 0 && (
                                        <SelectItem value="_none" disabled>No buses on this route</SelectItem>
                                    )}
                                    {filteredBusesForRoute.map(b => (
                                        <SelectItem key={b.id} value={b.id}>
                                            {b.registrationNumber || b.id}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        {/* Reporter dropdown (populated from users collection) */}
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label className="text-right">Reporter</Label>
                            <Select
                                value={formData.userId}
                                onValueChange={(val) => setFormData({ ...formData, userId: val })}
                            >
                                <SelectTrigger className="col-span-3">
                                    <SelectValue placeholder="Select reporter..." />
                                </SelectTrigger>
                                <SelectContent>
                                    {users.length === 0 && <SelectItem value="_none" disabled>No users found</SelectItem>}
                                    {users.map(u => (
                                        <SelectItem key={u.id} value={u.id}>
                                            {u.displayName || u.email || u.id}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        {/* Status selector */}
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label className="text-right">Status</Label>
                            <Select value={formData.status} onValueChange={(val) => setFormData({ ...formData, status: val })}>
                                <SelectTrigger className="col-span-3">
                                    <SelectValue placeholder="Select status" />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="OPEN">Open</SelectItem>
                                    <SelectItem value="RESOLVED">Resolved</SelectItem>
                                    <SelectItem value="CLAIMED">Claimed</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>

                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsDialogOpen(false)} disabled={isSubmitting}>Cancel</Button>
                        <Button onClick={handleSaveItem} disabled={isSubmitting}>
                            {isSubmitting ? "Saving..." : (editingId ? "Update Record" : "Save Record")}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
