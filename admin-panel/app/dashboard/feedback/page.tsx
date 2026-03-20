"use client";

/**
 * FeedbackPage — Admin module for viewing and managing passenger feedback.
 *
 * Features:
 * - Displays feedback cards with overall rating, sub-ratings, comment, and tags
 * - Search/filter across user, bus, route, comment text, and tags
 * - Create new feedback entries (linked to real users/routes/buses via dropdowns)
 * - Edit existing feedback via dialog
 * - Delete feedback with confirmation
 * - Resolves user display names and bus registration numbers for readable output
 *
 * Schema (db.md): { userId, busId, routeId, comment, ratings{}, tags[], createdAt }
 */

import { useEffect, useState, useCallback, useMemo } from "react";
import {
    collection, addDoc, updateDoc, deleteDoc, doc,
    getDocs, limit, query, serverTimestamp
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
    Dialog, DialogContent, DialogDescription,
    DialogFooter, DialogHeader, DialogTitle
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Star, MessageSquare, Bus, MapPin, Clock, User, Plus, Edit2, Trash2, Search, RefreshCcw } from "lucide-react";
import { toast } from "sonner";
import { format } from "date-fns";

// ─── Type Definitions ───────────────────────────────────────

/** Shape of the ratings sub-object in a feedback document */
export interface Ratings {
    overall: number;
    cleanliness: number;
    comfort: number;
    driver: number;
}

export interface Feedback {
    id: string;
    userId: string;
    busId: string;
    routeId: string;
    comment: string;
    ratings: Ratings;
    tags: string[];
    createdAt?: any;
    timestamp?: any;
}

export interface UserNode {
    id: string;
    displayName?: string;
    email?: string;
    [key: string]: any;
}

export interface BusNode {
    id: string;
    registrationNumber?: string;
    defaultRouteId?: string;
    routeId?: string;
    [key: string]: any;
}

export interface RouteNode {
    id: string;
    routeId?: string;
    name?: string;
    [key: string]: any;
}

/** Shape of the form data used in the create/edit dialog */
interface FeedbackFormData {
    userId: string;
    busId: string;
    routeId: string;
    comment: string;
    ratings: Ratings;
    tags: string; // Comma-separated string, converted to array on save
}

/** Default form values for creating a new feedback entry */
const DEFAULT_FORM: FeedbackFormData = {
    userId: "",
    busId: "",
    routeId: "",
    comment: "",
    ratings: { overall: 5, cleanliness: 5, comfort: 5, driver: 5 },
    tags: "",
};

export default function FeedbackPage() {
    // ─── State ──────────────────────────────────────────────
    const [feedbacks, setFeedbacks] = useState<Feedback[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState("");

    // Related collections (for dropdowns and display-name resolution)
    const [routes, setRoutes] = useState<RouteNode[]>([]);
    const [buses, setBuses] = useState<BusNode[]>([]);
    const [users, setUsers] = useState<UserNode[]>([]);

    // Dialog state for create/edit
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [editingId, setEditingId] = useState<string | null>(null);
    const [formData, setFormData] = useState<FeedbackFormData>({ ...DEFAULT_FORM });

    // ─── Lookup Maps ────────────────────────────────────────
    // Build quick-lookup maps so we can resolve IDs to display names in the card UI

    /** Maps userId → displayName */
    const userMap = useMemo(() => {
        const map: Record<string, string> = {};
        users.forEach(u => { map[u.id] = u.displayName || u.email || u.id; });
        return map;
    }, [users]);

    /** Maps busId → registrationNumber */
    const busMap = useMemo(() => {
        const map: Record<string, string> = {};
        buses.forEach(b => { map[b.id] = b.registrationNumber || b.id; });
        return map;
    }, [buses]);

    /** Maps routeId → route display label */
    const routeMap = useMemo(() => {
        const map: Record<string, string> = {};
        routes.forEach(r => { map[r.id] = r.routeId ? `${r.routeId} — ${r.name || ""}` : r.id; });
        return map;
    }, [routes]);

    // ─── Data Fetching ──────────────────────────────────────

    /**
     * Fetches the latest 100 feedback documents from Firestore.
     * Uses client-side sort (by createdAt or timestamp descending)
     * to avoid requiring a composite index.
     */
    const fetchFeedback = useCallback(async () => {
        try {
            setLoading(true);
            const q = query(collection(db, "feedback"), limit(100));
            const querySnapshot = await getDocs(q);

            const fetched: Feedback[] = [];
            querySnapshot.forEach((d) => {
                fetched.push({ id: d.id, ...d.data() } as Feedback);
            });

            // Sort newest first, supporting both Firestore Timestamp and ISO strings
            fetched.sort((a, b) => {
                const getTime = (item: Feedback) => {
                    const ts = item.createdAt || item.timestamp;
                    if (!ts) return 0;
                    if (typeof ts.toMillis === "function") return ts.toMillis();
                    const parsed = new Date(ts).getTime();
                    return isNaN(parsed) ? 0 : parsed;
                };
                return getTime(b) - getTime(a);
            });

            setFeedbacks(fetched);
        } catch (error) {
            console.error("Error fetching feedback:", error);
            toast.error("Failed to load feedback records.");
        } finally {
            setLoading(false);
        }
    }, []);

    /**
     * On mount, fetch feedback and all related collections (routes, buses, users).
     * Related collections are used for dropdown options and display-name resolution.
     */
    useEffect(() => {
        fetchFeedback();

        // Fetch routes for dropdown
        getDocs(collection(db, "routes")).then(snap => {
            const list: RouteNode[] = [];
            snap.forEach(d => list.push({ id: d.id, ...d.data() } as RouteNode));
            list.sort((a, b) => (a.routeId || "").localeCompare(b.routeId || "", undefined, { numeric: true }));
            setRoutes(list);
        });

        // Fetch buses for dropdown (cascading — filtered by selected route)
        getDocs(collection(db, "buses")).then(snap => {
            const list: BusNode[] = [];
            snap.forEach(d => list.push({ id: d.id, ...d.data() } as BusNode));
            setBuses(list);
        });

        // Fetch users for dropdown
        getDocs(collection(db, "users")).then(snap => {
            const list: UserNode[] = [];
            snap.forEach(d => list.push({ id: d.id, ...d.data() } as UserNode));
            setUsers(list);
        });
    }, [fetchFeedback]);

    // ─── Filtered Feedback (search) ─────────────────────────

    /** Filter feedbacks based on search query across multiple fields */
    const filteredFeedbacks = useMemo(() => {
        if (!searchQuery.trim()) return feedbacks;
        const q = searchQuery.toLowerCase();
        return feedbacks.filter(f =>
            (f.userId || "").toLowerCase().includes(q) ||
            (userMap[f.userId] || "").toLowerCase().includes(q) ||
            (f.busId || "").toLowerCase().includes(q) ||
            (busMap[f.busId] || "").toLowerCase().includes(q) ||
            (f.routeId || "").toLowerCase().includes(q) ||
            (f.comment || "").toLowerCase().includes(q) ||
            (f.tags || []).some((t: string) => t.toLowerCase().includes(q))
        );
    }, [feedbacks, searchQuery, userMap, busMap]);

    // ─── Helpers ────────────────────────────────────────────

    /** Render 1–5 star icons, filled up to the given rating */
    const renderStars = (rating: number = 0) => (
        <div className="flex">
            {[1, 2, 3, 4, 5].map((star) => (
                <Star
                    key={star}
                    className={`h-4 w-4 ${star <= rating ? "text-yellow-400 fill-yellow-400" : "text-zinc-200 dark:text-zinc-800"}`}
                />
            ))}
        </div>
    );

    /** Safely format a Firestore Timestamp or ISO string to a readable date */
    const formatTimestamp = (item: Feedback) => {
        const ts = item.createdAt || item.timestamp;
        if (!ts) return "Unknown time";
        if (typeof ts.toDate === "function") return format(ts.toDate(), "MMM d, h:mm a");
        const parsed = new Date(ts);
        if (!isNaN(parsed.getTime())) return format(parsed, "MMM d, h:mm a");
        return "Invalid date";
    };

    /** Clamp a rating value between 1 and 5 */
    const clampRating = (value: number) => Math.max(1, Math.min(5, value));

    // ─── Dialog Handlers ────────────────────────────────────

    /**
     * Opens the create/edit dialog.
     * If `fbToEdit` is provided, populates form with existing data (edit mode).
     * Otherwise, resets form to defaults (create mode).
     */
    const handleOpenDialog = (fbToEdit?: Feedback) => {
        if (fbToEdit) {
            setEditingId(fbToEdit.id);
            setFormData({
                userId: fbToEdit.userId || "",
                busId: fbToEdit.busId || "",
                routeId: fbToEdit.routeId || "",
                comment: fbToEdit.comment || "",
                ratings: {
                    overall: fbToEdit.ratings?.overall || 5,
                    cleanliness: fbToEdit.ratings?.cleanliness || 5,
                    comfort: fbToEdit.ratings?.comfort || 5,
                    driver: fbToEdit.ratings?.driver || 5
                },
                tags: (fbToEdit.tags || []).join(", ")
            });
        } else {
            setEditingId(null);
            setFormData({ ...DEFAULT_FORM });
        }
        setIsDialogOpen(true);
    };

    /**
     * Saves feedback to Firestore (create or update).
     * Converts comma-separated tags string to array.
     * Clamps all ratings to 1–5 range.
     */
    const handleSaveFeedback = async () => {
        // Validate required fields
        if (!formData.userId) {
            toast.error("Please select a user.");
            return;
        }
        if (!formData.comment.trim()) {
            toast.error("A comment is required.");
            return;
        }

        try {
            setIsSubmitting(true);

            // Convert tags from comma-separated string to trimmed array
            const tagsArray = formData.tags
                .split(",")
                .map(t => t.trim())
                .filter(t => t.length > 0);

            // Build document data matching db.md schema
            const fbData = {
                userId: formData.userId,
                busId: formData.busId,
                routeId: formData.routeId,
                comment: formData.comment.trim(),
                ratings: {
                    overall: clampRating(Number(formData.ratings.overall)),
                    cleanliness: clampRating(Number(formData.ratings.cleanliness)),
                    comfort: clampRating(Number(formData.ratings.comfort)),
                    driver: clampRating(Number(formData.ratings.driver)),
                },
                tags: tagsArray,
                createdAt: serverTimestamp(),
            };

            if (editingId) {
                // Update existing document
                await updateDoc(doc(db, "feedback", editingId), fbData);
                toast.success("Feedback updated successfully.");
            } else {
                // Create new document with auto-generated ID
                await addDoc(collection(db, "feedback"), fbData);
                toast.success("Feedback created successfully.");
            }

            setIsDialogOpen(false);
            fetchFeedback();
        } catch (error) {
            console.error("Error saving feedback:", error);
            toast.error("Failed to save feedback.");
        } finally {
            setIsSubmitting(false);
        }
    };

    /**
     * Deletes a feedback document after user confirmation.
     * @param id - Firestore document ID to delete
     */
    const handleDeleteFeedback = async (id: string) => {
        if (!confirm("Are you sure you want to delete this feedback record?")) return;

        try {
            await deleteDoc(doc(db, "feedback", id));
            toast.success("Feedback deleted.");
            fetchFeedback();
        } catch (error) {
            console.error("Error deleting feedback:", error);
            toast.error("Failed to delete feedback.");
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
                    <h2 className="text-3xl font-bold tracking-tight">Passenger Feedback</h2>
                    <p className="text-muted-foreground mt-1">
                        Review ratings and comments submitted by commuters across the network.
                    </p>
                </div>
                <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={fetchFeedback} disabled={loading}>
                        <RefreshCcw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                        Refresh
                    </Button>
                    <Button onClick={() => handleOpenDialog()}>
                        <Plus className="h-4 w-4 mr-2" />
                        Add Feedback
                    </Button>
                </div>
            </div>

            {/* ─── Search Bar ────────────────────────────────── */}
            <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-500" />
                <Input
                    placeholder="Search by user, bus, route, comment, or tag..."
                    className="pl-10"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                />
            </div>

            {/* ─── Results Summary ────────────────────────────── */}
            {!loading && feedbacks.length > 0 && (
                <div className="text-sm text-zinc-500">
                    Showing {filteredFeedbacks.length} of {feedbacks.length} feedback entries
                    {searchQuery && ` matching "${searchQuery}"`}
                </div>
            )}

            {/* ─── Feedback List ─────────────────────────────── */}
            {loading ? (
                /* Loading skeleton */
                <div className="grid gap-6">
                    {[1, 2, 3].map(i => (
                        <Card key={i} className="overflow-hidden animate-pulse">
                            <div className="flex flex-col md:flex-row">
                                <div className="bg-zinc-100 dark:bg-zinc-900/50 p-6 md:w-48 flex flex-col items-center justify-center">
                                    <div className="h-10 w-16 bg-zinc-200 dark:bg-zinc-700 rounded mb-2" />
                                    <div className="flex gap-1">
                                        {[1, 2, 3, 4, 5].map(s => <div key={s} className="h-4 w-4 bg-zinc-200 dark:bg-zinc-700 rounded" />)}
                                    </div>
                                </div>
                                <div className="flex-1 p-6 space-y-3">
                                    <div className="h-4 w-1/3 bg-zinc-200 dark:bg-zinc-700 rounded" />
                                    <div className="h-3 w-2/3 bg-zinc-100 dark:bg-zinc-800 rounded" />
                                    <div className="h-16 w-full bg-zinc-50 dark:bg-zinc-900/50 rounded" />
                                </div>
                            </div>
                        </Card>
                    ))}
                </div>
            ) : filteredFeedbacks.length === 0 ? (
                /* Empty state */
                <div className="flex flex-col items-center justify-center py-16 text-center text-zinc-500 bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800 rounded-lg">
                    <MessageSquare className="h-12 w-12 mb-4 opacity-20" />
                    <p className="text-lg font-medium">
                        {searchQuery ? "No matching feedback" : "No feedback recorded"}
                    </p>
                    <p className="text-sm">
                        {searchQuery
                            ? `No results for "${searchQuery}". Try a different search term.`
                            : "When passengers rate their trips, they will appear here."}
                    </p>
                </div>
            ) : (
                /* Feedback cards grid */
                <div className="grid gap-6">
                    {filteredFeedbacks.map((f) => (
                        <Card key={f.id} className="overflow-hidden group">
                            <div className="flex flex-col md:flex-row relative">
                                {/* ─── Hover Action Buttons (top-right) ── */}
                                <div className="absolute top-4 right-4 flex space-x-1 opacity-100 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity z-10">
                                    <Button variant="outline" size="icon" className="h-8 w-8 bg-white/80 backdrop-blur-sm dark:bg-zinc-900/80" onClick={() => handleOpenDialog(f)}>
                                        <Edit2 className="h-4 w-4" />
                                    </Button>
                                    <Button variant="outline" size="icon" className="h-8 w-8 bg-white/80 backdrop-blur-sm dark:bg-zinc-900/80 text-red-500 hover:text-red-600" onClick={() => handleDeleteFeedback(f.id)}>
                                        <Trash2 className="h-4 w-4" />
                                    </Button>
                                </div>

                                {/* ─── Left: Overall Rating Panel ────── */}
                                <div className="bg-zinc-50 dark:bg-zinc-900/50 p-6 flex flex-col items-center justify-center md:w-48 border-b md:border-b-0 md:border-r border-zinc-100 dark:border-zinc-800">
                                    <div className="text-4xl font-bold mb-2 text-zinc-900 dark:text-zinc-50">
                                        {f.ratings?.overall || 0}<span className="text-lg text-zinc-400 font-normal">/5</span>
                                    </div>
                                    {renderStars(f.ratings?.overall)}
                                    {/* Tags below the stars */}
                                    {f.tags && f.tags.length > 0 && (
                                        <div className="mt-4 flex flex-wrap gap-1 justify-center">
                                            {f.tags.map((tag: string, i: number) => (
                                                <Badge key={i} variant="secondary" className="text-[10px] px-1.5 py-0">{tag}</Badge>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                {/* ─── Right: Details & Comment ───────── */}
                                <div className="flex-1 p-6 flex flex-col">
                                    {/* User, bus, route, and time metadata */}
                                    <div className="flex justify-between items-start mb-4 pr-16 md:pr-20 mt-4 md:mt-0">
                                        <div className="space-y-1 w-full">
                                            <div className="flex items-center text-sm font-medium text-zinc-900 dark:text-zinc-100">
                                                <User className="h-4 w-4 mr-2 text-zinc-500" />
                                                <span>{userMap[f.userId] || f.userId || "Anonymous Passenger"}</span>
                                            </div>
                                            <div className="flex flex-wrap items-center text-xs text-zinc-500 gap-y-2 gap-x-4">
                                                <span className="flex items-center whitespace-nowrap">
                                                    <Bus className="h-3 w-3 mr-1" /> {busMap[f.busId] || f.busId || "Unknown Bus"}
                                                </span>
                                                <span className="flex items-center whitespace-nowrap">
                                                    <MapPin className="h-3 w-3 mr-1" /> {routeMap[f.routeId] || f.routeId || "N/A"}
                                                </span>
                                                <span className="flex items-center whitespace-nowrap">
                                                    <Clock className="h-3 w-3 mr-1" /> {formatTimestamp(f)}
                                                </span>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Comment text */}
                                    <div className="bg-zinc-50 dark:bg-zinc-900/50 p-4 rounded-md text-sm text-zinc-700 dark:text-zinc-300 italic mb-4 flex-1">
                                        &ldquo;{f.comment || "No written comment provided."}&rdquo;
                                    </div>

                                    {/* Sub-ratings grid */}
                                    <div className="grid grid-cols-3 gap-4 pt-4 border-t border-zinc-100 dark:border-zinc-800">
                                        <div className="space-y-1">
                                            <p className="text-xs text-zinc-500 uppercase tracking-wider font-semibold">Cleanliness</p>
                                            <div className="flex items-center">
                                                <span className="text-sm font-medium mr-1.5">{f.ratings?.cleanliness || "-"}</span>
                                                <Star className="h-3 w-3 text-yellow-400 fill-yellow-400" />
                                            </div>
                                        </div>
                                        <div className="space-y-1">
                                            <p className="text-xs text-zinc-500 uppercase tracking-wider font-semibold">Comfort</p>
                                            <div className="flex items-center">
                                                <span className="text-sm font-medium mr-1.5">{f.ratings?.comfort || "-"}</span>
                                                <Star className="h-3 w-3 text-yellow-400 fill-yellow-400" />
                                            </div>
                                        </div>
                                        <div className="space-y-1">
                                            <p className="text-xs text-zinc-500 uppercase tracking-wider font-semibold">Driver</p>
                                            <div className="flex items-center">
                                                <span className="text-sm font-medium mr-1.5">{f.ratings?.driver || "-"}</span>
                                                <Star className="h-3 w-3 text-yellow-400 fill-yellow-400" />
                                            </div>
                                        </div>
                                    </div>

                                    {/* Mobile-only action buttons (visible on small screens) */}
                                    <div className="sm:hidden flex gap-2 mt-4">
                                        <Button variant="outline" size="sm" className="flex-1" onClick={() => handleOpenDialog(f)}>
                                            <Edit2 className="h-3.5 w-3.5 mr-2" /> Edit
                                        </Button>
                                        <Button variant="outline" size="sm" className="flex-1 text-red-500 hover:text-red-600 hover:bg-red-50" onClick={() => handleDeleteFeedback(f.id)}>
                                            <Trash2 className="h-3.5 w-3.5 mr-2" /> Delete
                                        </Button>
                                    </div>
                                </div>
                            </div>
                        </Card>
                    ))}
                </div>
            )}

            {/* ═══════════════════════════════════════════════════ */}
            {/* ─── CREATE / EDIT DIALOG ─────────────────────── */}
            {/* ═══════════════════════════════════════════════════ */}
            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
                <DialogContent className="sm:max-w-[500px]">
                    <DialogHeader>
                        <DialogTitle>{editingId ? "Edit Feedback" : "Add Feedback"}</DialogTitle>
                        <DialogDescription>
                            {editingId
                                ? "Update the feedback details below."
                                : "Create a new feedback entry linked to a user, route, and bus."}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="grid gap-4 py-4 max-h-[60vh] overflow-y-auto pr-2">
                        {/* User selector */}
                        <div className="grid gap-2">
                            <Label htmlFor="userId">User *</Label>
                            <Select value={formData.userId} onValueChange={(val) => setFormData({ ...formData, userId: val })}>
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

                        {/* Route → Bus cascading selectors */}
                        <div className="grid grid-cols-2 gap-4">
                            <div className="grid gap-2">
                                <Label htmlFor="routeId">Route</Label>
                                <Select
                                    value={formData.routeId}
                                    onValueChange={(val) => setFormData({ ...formData, routeId: val, busId: "" })}
                                >
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select a route..." />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {routes.map(r => (
                                            <SelectItem key={r.id} value={r.id}>
                                                {r.routeId || r.id} — {r.name || ""}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                            <div className="grid gap-2">
                                <Label htmlFor="busId">Bus</Label>
                                <Select
                                    value={formData.busId}
                                    onValueChange={(val) => setFormData({ ...formData, busId: val })}
                                    disabled={!formData.routeId}
                                >
                                    <SelectTrigger>
                                        <SelectValue placeholder={formData.routeId ? "Select a bus..." : "Select route first"} />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {/* Filter buses by the selected route */}
                                        {buses
                                            .filter(b => !formData.routeId || b.defaultRouteId === formData.routeId || b.routeId === formData.routeId)
                                            .map(b => (
                                                <SelectItem key={b.id} value={b.id}>
                                                    {b.registrationNumber || b.id}
                                                </SelectItem>
                                            ))
                                        }
                                        {buses.filter(b => !formData.routeId || b.defaultRouteId === formData.routeId || b.routeId === formData.routeId).length === 0 && (
                                            <SelectItem value="_none" disabled>No buses on this route</SelectItem>
                                        )}
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>

                        {/* Ratings grid (1–5 for each sub-category) */}
                        <div className="border border-zinc-200 dark:border-zinc-800 rounded-md p-4 space-y-4 bg-zinc-50 dark:bg-zinc-900/30">
                            <p className="text-sm font-medium">Ratings (1–5)</p>
                            <div className="grid grid-cols-2 gap-4">
                                {(["overall", "cleanliness", "comfort", "driver"] as const).map((key) => (
                                    <div key={key} className="grid gap-2">
                                        <Label className="text-xs text-zinc-500 capitalize">{key}</Label>
                                        <Input
                                            type="number"
                                            min={1}
                                            max={5}
                                            value={formData.ratings[key]}
                                            onChange={(e) => setFormData({
                                                ...formData,
                                                ratings: {
                                                    ...formData.ratings,
                                                    [key]: clampRating(Number(e.target.value))
                                                }
                                            })}
                                        />
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Comment text */}
                        <div className="grid gap-2">
                            <Label htmlFor="comment">Comment *</Label>
                            <Textarea
                                id="comment"
                                value={formData.comment}
                                onChange={(e) => setFormData({ ...formData, comment: e.target.value })}
                                placeholder="Passenger's written review..."
                                rows={3}
                            />
                        </div>

                        {/* Tags (comma-separated) */}
                        <div className="grid gap-2">
                            <Label htmlFor="tags">Tags (comma-separated)</Label>
                            <Input
                                id="tags"
                                value={formData.tags}
                                onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                                placeholder="e.g. Clean, On-time, Friendly"
                            />
                        </div>
                    </div>

                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsDialogOpen(false)} disabled={isSubmitting}>
                            Cancel
                        </Button>
                        <Button onClick={handleSaveFeedback} disabled={isSubmitting}>
                            {isSubmitting ? "Saving..." : "Save Feedback"}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
