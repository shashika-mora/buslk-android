"use client";

/**
 * BroadcastsPage — Admin module for creating and managing broadcast notifications.
 *
 * Features:
 * - Compose new broadcasts targeting all users or specific routes
 * - Route selection via dropdown (fetched from Firestore `routes` collection)
 * - Live Android-style notification preview
 * - Broadcast history with edit and delete support
 * - Schema-aligned with db.md: uses `type`, `priority`, `targetRoutes`, `active`
 */

export interface Route {
    id: string;
    routeId: string;
    name: string;
    startLocation?: string;
    endLocation?: string;
    [key: string]: any;
}

export interface Broadcast {
    id: string;
    title: string;
    message: string;
    type: string;
    priority: string;
    targetRoutes?: string[];
    targetAudience?: string;
    targetRoute?: string;
    active?: boolean;
    createdAt?: any;
    timestamp?: any;
}

import { useState, useEffect } from "react";
import {
    collection, addDoc, updateDoc, deleteDoc, doc,
    serverTimestamp, query, limit, getDocs
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import {
    Dialog, DialogContent, DialogDescription,
    DialogFooter, DialogHeader, DialogTitle
} from "@/components/ui/dialog";
import { Send, History, Radio, RefreshCcw, Bell, Edit2, Trash2, Megaphone } from "lucide-react";
import { toast } from "sonner";
import { format } from "date-fns";

export default function BroadcastsPage() {
    // ─── State ──────────────────────────────────────────────
    const [activeTab, setActiveTab] = useState("compose"); // "compose" or "history"

    // Compose form state
    const [title, setTitle] = useState("");
    const [message, setMessage] = useState("");
    const [type, setType] = useState("INFO");               // INFO, WARNING, ALERT
    const [priority, setPriority] = useState("MEDIUM");      // LOW, MEDIUM, HIGH
    const [targetAudience, setTargetAudience] = useState("all"); // "all" or "route"
    const [targetRoutes, setTargetRoutes] = useState<string[]>([]); // Selected route IDs
    const [isSending, setIsSending] = useState(false);

    // History state
    const [history, setHistory] = useState<Broadcast[]>([]);
    const [loadingHistory, setLoadingHistory] = useState(true);

    // Edit dialog state
    const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
    const [editingBroadcast, setEditingBroadcast] = useState<Broadcast | null>(null);
    const [editForm, setEditForm] = useState({
        title: "", message: "", type: "INFO", priority: "MEDIUM",
        targetRoutes: [] as string[], active: true
    });

    // Routes fetched from Firestore for the dropdown
    const [routes, setRoutes] = useState<Route[]>([]);

    // ─── Data Fetching ──────────────────────────────────────

    /** Fetch all routes for the route selection dropdown */
    useEffect(() => {
        getDocs(collection(db, "routes")).then(snap => {
            const list: Route[] = [];
            snap.forEach(d => list.push({ id: d.id, ...d.data() } as Route));
            // Sort routes by routeId for easier browsing
            list.sort((a, b) => (a.routeId || "").localeCompare(b.routeId || "", undefined, { numeric: true }));
            setRoutes(list);
        });
    }, []);

    /**
     * Fetch broadcast history from Firestore.
     * Uses client-side sort to avoid needing a composite index.
     * Called on mount and when switching to the history tab.
     */
    const fetchHistory = async () => {
        try {
            setLoadingHistory(true);
            const q = query(collection(db, "broadcasts"), limit(50));
            const querySnapshot = await getDocs(q);
            const fetched = querySnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Broadcast));

            // Sort by createdAt (newest first), falling back to timestamp for older records
            fetched.sort((a, b) => {
                const timeA = a.createdAt?.toMillis?.() || a.timestamp?.toMillis?.() || 0;
                const timeB = b.createdAt?.toMillis?.() || b.timestamp?.toMillis?.() || 0;
                return timeB - timeA;
            });

            setHistory(fetched);
        } catch (error) {
            console.error("Error fetching broadcast history:", error);
            toast.error("Failed to load broadcast history.");
        } finally {
            setLoadingHistory(false);
        }
    };

    // Load history on mount and when tab changes
    useEffect(() => {
        fetchHistory();
    }, []);

    // ─── Compose Handler ────────────────────────────────────

    /**
     * Validates form inputs and writes a new broadcast document to Firestore.
     * Schema follows db.md: { title, message, type, priority, targetRoutes, active, createdAt }
     * In production, a Cloud Function would pick this up and dispatch via FCM.
     */
    const handleSendBroadcast = async () => {
        // Validation
        if (!title.trim() || !message.trim()) {
            toast.error("Title and message are required.");
            return;
        }
        if (targetAudience === "route" && targetRoutes.length === 0) {
            toast.error("Please select at least one target route.");
            return;
        }

        try {
            setIsSending(true);

            // Build broadcast document matching db.md schema
            const broadcastData = {
                title: title.trim(),
                message: message.trim(),
                type,                                              // INFO, WARNING, ALERT
                priority,                                          // LOW, MEDIUM, HIGH
                targetRoutes: targetAudience === "route" ? targetRoutes : [],  // Empty = all users
                active: true,
                createdAt: serverTimestamp(),
            };

            await addDoc(collection(db, "broadcasts"), broadcastData);
            toast.success("Broadcast created successfully.");

            // Reset form to defaults
            setTitle("");
            setMessage("");
            setType("INFO");
            setPriority("MEDIUM");
            setTargetAudience("all");
            setTargetRoutes([]);

            // Refresh history if visible
            fetchHistory();
        } catch (error) {
            console.error("Error sending broadcast:", error);
            toast.error("Failed to create broadcast.");
        } finally {
            setIsSending(false);
        }
    };

    // ─── Edit Handler ───────────────────────────────────────

    /** Opens the edit dialog with the selected broadcast's data pre-filled */
    const handleOpenEdit = (broadcast: Broadcast) => {
        setEditingBroadcast(broadcast);
        setEditForm({
            title: broadcast.title || "",
            message: broadcast.message || "",
            type: broadcast.type || "INFO",
            priority: broadcast.priority || "MEDIUM",
            targetRoutes: broadcast.targetRoutes || [],
            active: broadcast.active !== undefined ? broadcast.active : true,
        });
        setIsEditDialogOpen(true);
    };

    /** Saves edits to an existing broadcast document */
    const handleSaveEdit = async () => {
        if (!editingBroadcast) return;
        try {
            await updateDoc(doc(db, "broadcasts", editingBroadcast.id), {
                title: editForm.title.trim(),
                message: editForm.message.trim(),
                type: editForm.type,
                priority: editForm.priority,
                targetRoutes: editForm.targetRoutes,
                active: editForm.active,
            });
            toast.success("Broadcast updated.");
            setIsEditDialogOpen(false);
            fetchHistory();
        } catch (error) {
            console.error("Error updating broadcast:", error);
            toast.error("Failed to update broadcast.");
        }
    };

    // ─── Delete Handler ─────────────────────────────────────

    /** Deletes a broadcast document after user confirmation */
    const handleDelete = async (id: string) => {
        if (!confirm("Are you sure you want to delete this broadcast?")) return;
        try {
            await deleteDoc(doc(db, "broadcasts", id));
            toast.success("Broadcast deleted.");
            fetchHistory();
        } catch (error) {
            console.error("Error deleting broadcast:", error);
            toast.error("Failed to delete broadcast.");
        }
    };

    // ─── Helper: Format timestamp safely ────────────────────
    const formatTimestamp = (item: Broadcast) => {
        const ts = item.createdAt || item.timestamp;
        if (!ts) return "Just now";
        if (typeof ts.toDate === "function") return format(ts.toDate(), "MMM d, yyyy h:mm a");
        return "Saved";
    };

    // ─── Helper: Badge color for broadcast type ─────────────
    const getTypeBadge = (t: string) => {
        switch (t) {
            case "WARNING": return <Badge className="bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400">Warning</Badge>;
            case "ALERT": return <Badge variant="destructive">Alert</Badge>;
            default: return <Badge className="bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400">Info</Badge>;
        }
    };

    // ─── Helper: Badge for priority level ───────────────────
    const getPriorityBadge = (p: string) => {
        switch (p) {
            case "HIGH": return <Badge variant="destructive" className="text-xs">High</Badge>;
            case "LOW": return <Badge variant="outline" className="text-xs">Low</Badge>;
            default: return <Badge variant="secondary" className="text-xs">Medium</Badge>;
        }
    };

    // ═══════════════════════════════════════════════════════════
    // ─── RENDER ─────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════

    return (
        <div className="space-y-6 max-w-5xl mx-auto">
            {/* ─── Page Header ───────────────────────────────── */}
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center space-y-4 md:space-y-0">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">Broadcasts</h2>
                    <p className="text-muted-foreground mt-1">Send push notifications to commuters or manage history.</p>
                </div>
                <Button variant="outline" size="sm" onClick={fetchHistory} disabled={loadingHistory}>
                    <RefreshCcw className={`h-4 w-4 mr-2 ${loadingHistory ? "animate-spin" : ""}`} />
                    Refresh
                </Button>
            </div>

            {/* ─── Tab Navigation ────────────────────────────── */}
            <div className="flex space-x-2 border-b border-zinc-200 dark:border-zinc-800 pb-px">
                <button
                    onClick={() => setActiveTab("compose")}
                    className={`flex items-center px-4 py-2 border-b-2 text-sm font-medium transition-colors ${activeTab === "compose" ? "border-blue-600 text-blue-600 dark:border-blue-400 dark:text-blue-400" : "border-transparent text-zinc-500 hover:text-zinc-700 hover:border-zinc-300 dark:hover:text-zinc-300 dark:hover:border-zinc-700"}`}
                >
                    <Send className="w-4 h-4 mr-2" />
                    Compose
                </button>
                <button
                    onClick={() => setActiveTab("history")}
                    className={`flex items-center px-4 py-2 border-b-2 text-sm font-medium transition-colors ${activeTab === "history" ? "border-blue-600 text-blue-600 dark:border-blue-400 dark:text-blue-400" : "border-transparent text-zinc-500 hover:text-zinc-700 hover:border-zinc-300 dark:hover:text-zinc-300 dark:hover:border-zinc-700"}`}
                >
                    <History className="w-4 h-4 mr-2" />
                    History ({history.length})
                </button>
            </div>

            {/* ═══════════════════════════════════════════════════ */}
            {/* ─── COMPOSE TAB ──────────────────────────────── */}
            {/* ═══════════════════════════════════════════════════ */}
            {activeTab === "compose" ? (
                <div className="grid md:grid-cols-2 gap-6">
                    {/* ─── Compose Form Card ─────────────────── */}
                    <Card className="border-zinc-200 dark:border-zinc-800">
                        <CardHeader>
                            <CardTitle>New Broadcast</CardTitle>
                            <CardDescription>Target specific routes or all registered users.</CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            {/* Title input */}
                            <div className="space-y-2">
                                <Label>Notification Title</Label>
                                <Input
                                    placeholder="e.g. Schedule Change: Route 138"
                                    value={title}
                                    onChange={(e) => setTitle(e.target.value)}
                                    maxLength={60}
                                />
                                <div className="text-xs text-right text-zinc-500">{title.length}/60</div>
                            </div>

                            {/* Message body */}
                            <div className="space-y-2">
                                <Label>Message Body</Label>
                                <Textarea
                                    placeholder="Enter the main content of your notification..."
                                    className="min-h-[120px] resize-y"
                                    value={message}
                                    onChange={(e) => setMessage(e.target.value)}
                                    maxLength={250}
                                />
                                <div className="text-xs text-right text-zinc-500">{message.length}/250</div>
                            </div>

                            {/* Type and Priority selectors (side by side) */}
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label>Type</Label>
                                    <Select value={type} onValueChange={setType}>
                                        <SelectTrigger><SelectValue /></SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="INFO">ℹ️ Info</SelectItem>
                                            <SelectItem value="WARNING">⚠️ Warning</SelectItem>
                                            <SelectItem value="ALERT">🚨 Alert</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="space-y-2">
                                    <Label>Priority</Label>
                                    <Select value={priority} onValueChange={setPriority}>
                                        <SelectTrigger><SelectValue /></SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="LOW">Low</SelectItem>
                                            <SelectItem value="MEDIUM">Medium</SelectItem>
                                            <SelectItem value="HIGH">High</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            </div>

                            {/* Target audience selection */}
                            <div className="space-y-3 pt-2">
                                <Label>Target Audience</Label>
                                <RadioGroup value={targetAudience} onValueChange={(v) => { setTargetAudience(v); if (v === "all") setTargetRoutes([]); }} className="space-y-2">
                                    <div className="flex items-center space-x-2 border p-3 rounded-md border-zinc-200 dark:border-zinc-800 cursor-pointer hover:bg-zinc-50 dark:hover:bg-zinc-900/50" onClick={() => { setTargetAudience("all"); setTargetRoutes([]); }}>
                                        <RadioGroupItem value="all" id="all" />
                                        <Label htmlFor="all" className="flex-1 cursor-pointer">All Users (Global Broadcast)</Label>
                                    </div>
                                    <div className="flex items-center space-x-2 border p-3 rounded-md border-zinc-200 dark:border-zinc-800 cursor-pointer hover:bg-zinc-50 dark:hover:bg-zinc-900/50" onClick={() => setTargetAudience("route")}>
                                        <RadioGroupItem value="route" id="route" />
                                        <Label htmlFor="route" className="flex-1 cursor-pointer">Specific Route Subscribers</Label>
                                    </div>
                                </RadioGroup>
                            </div>

                            {/* Route dropdown (shown only when targeting a route) */}
                            {targetAudience === "route" && (
                                <div className="space-y-2 pt-2 animate-in fade-in slide-in-from-top-2">
                                    <Label>Select Route</Label>
                                    <Select onValueChange={(v) => setTargetRoutes([v])}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Choose a route..." />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {routes.map(r => (
                                                <SelectItem key={r.id} value={r.routeId || r.id}>
                                                    {r.routeId} — {r.name}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                            )}
                        </CardContent>

                        {/* Submit button */}
                        <CardFooter className="bg-zinc-50 dark:bg-zinc-900/50 border-t border-zinc-100 dark:border-zinc-800 flex justify-end p-4">
                            <Button
                                onClick={handleSendBroadcast}
                                disabled={isSending || !title || !message}
                                className="w-full md:w-auto"
                            >
                                {isSending ? (
                                    <>
                                        <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />
                                        Sending...
                                    </>
                                ) : (
                                    <>
                                        <Megaphone className="mr-2 h-4 w-4" />
                                        Send Broadcast
                                    </>
                                )}
                            </Button>
                        </CardFooter>
                    </Card>

                    {/* ─── Preview Panel (Android-style) ──────── */}
                    <div className="space-y-4">
                        <Label className="text-muted-foreground">Preview (Android)</Label>
                        <div className="w-[300px] h-fit bg-slate-900 rounded-[2rem] p-3 shadow-2xl relative overflow-hidden mx-auto flex-shrink-0 border-4 border-slate-800">
                            {/* Device status bar mockup */}
                            <div className="flex justify-between items-center px-4 pt-1 pb-3 text-[10px] text-white/80 font-medium">
                                <span>12:00</span>
                                <div className="flex space-x-1 items-center">
                                    <span className="w-3 h-3 rounded-full bg-white/40" />
                                    <span className="w-3 h-3 rounded-full bg-white/40" />
                                </div>
                            </div>

                            {/* Notification card preview */}
                            <div className="bg-slate-800/80 backdrop-blur-md rounded-2xl p-4 shadow-lg border border-slate-700/50">
                                <div className="flex items-center space-x-2 mb-2">
                                    <div className="bg-blue-600 rounded p-1">
                                        <Bell className="w-3 h-3 text-white" />
                                    </div>
                                    <span className="text-white/60 text-xs font-medium">BusLK</span>
                                    <span className="text-white/40 text-[10px] ml-auto">Now</span>
                                </div>
                                <h4 className="text-white font-semibold text-sm leading-tight mb-1">
                                    {title || "Notification Title"}
                                </h4>
                                <p className="text-white/70 text-xs leading-snug line-clamp-3">
                                    {message || "The notification message body will appear here. It should be concise and direct."}
                                </p>
                            </div>

                            {/* Background gradient for mock device */}
                            <div className="absolute inset-0 z-[-1] opacity-20 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-blue-900 via-slate-900 to-black"></div>
                        </div>

                        {/* Info note about FCM delivery */}
                        <div className="bg-blue-50 dark:bg-blue-950/30 p-4 rounded-lg border border-blue-100 dark:border-blue-900 mt-6">
                            <h4 className="font-semibold text-blue-800 dark:text-blue-300 mb-2 flex items-center text-sm">
                                <Bell className="w-4 h-4 mr-2" /> Note on Delivery
                            </h4>
                            <p className="text-blue-700/80 dark:text-blue-400/80 text-xs leading-relaxed">
                                Broadcasts are saved to Firestore. To deliver them as push notifications
                                to devices, a Firebase Cloud Function must listen to the <code className="bg-blue-100 dark:bg-blue-900/50 px-1 rounded">broadcasts</code> collection
                                and dispatch via Firebase Cloud Messaging (FCM).
                            </p>
                        </div>
                    </div>
                </div>
            ) : (
                /* ═══════════════════════════════════════════════ */
                /* ─── HISTORY TAB ──────────────────────────── */
                /* ═══════════════════════════════════════════════ */
                <Card className="border-zinc-200 dark:border-zinc-800">
                    <CardHeader className="flex flex-row items-center justify-between">
                        <div>
                            <CardTitle>Broadcast History</CardTitle>
                            <CardDescription>All notifications created from the admin panel.</CardDescription>
                        </div>
                        <Button variant="outline" size="sm" onClick={fetchHistory} disabled={loadingHistory}>
                            <RefreshCcw className={`h-4 w-4 mr-2 ${loadingHistory ? 'animate-spin' : ''}`} />
                            Refresh
                        </Button>
                    </CardHeader>
                    <CardContent>
                        {loadingHistory ? (
                            /* Loading skeleton */
                            <div className="space-y-3">
                                {[1, 2, 3].map(i => (
                                    <div key={i} className="animate-pulse p-4 border rounded-lg">
                                        <div className="flex gap-2 mb-2">
                                            <div className="h-5 w-16 bg-zinc-200 dark:bg-zinc-700 rounded" />
                                            <div className="h-5 w-12 bg-zinc-200 dark:bg-zinc-700 rounded" />
                                        </div>
                                        <div className="h-4 w-3/4 bg-zinc-200 dark:bg-zinc-700 rounded mb-2" />
                                        <div className="h-3 w-1/2 bg-zinc-100 dark:bg-zinc-800 rounded" />
                                    </div>
                                ))}
                            </div>
                        ) : history.length === 0 ? (
                            /* Empty state */
                            <div className="text-center py-12 text-zinc-500 bg-zinc-50 dark:bg-zinc-900/50 rounded-lg">
                                <History className="w-8 h-8 opacity-20 mx-auto mb-3" />
                                <p>No broadcasts found.</p>
                            </div>
                        ) : (
                            /* History list */
                            <div className="space-y-4">
                                {history.map((item) => (
                                    <div key={item.id} className="p-4 border border-zinc-200 dark:border-zinc-800 rounded-lg hover:bg-zinc-50 dark:hover:bg-zinc-900/30 transition-colors">
                                        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                                            <div className="space-y-1 flex-1">
                                                {/* Badges row: type, priority, active status, target */}
                                                <div className="flex flex-wrap items-center gap-2 mb-1">
                                                    {getTypeBadge(item.type || "INFO")}
                                                    {getPriorityBadge(item.priority || "MEDIUM")}
                                                    {item.active === false && (
                                                        <Badge variant="outline" className="text-xs text-zinc-500">Inactive</Badge>
                                                    )}
                                                    {/* Show target routes or Global */}
                                                    {item.targetRoutes && item.targetRoutes.length > 0 ? (
                                                        <span className="text-xs text-zinc-500">
                                                            Routes: {item.targetRoutes.join(", ")}
                                                        </span>
                                                    ) : item.targetAudience === "route" && item.targetRoute ? (
                                                        <span className="text-xs text-zinc-500">Route: {item.targetRoute}</span>
                                                    ) : (
                                                        <span className="text-xs text-zinc-500">🌍 Global</span>
                                                    )}
                                                </div>
                                                {/* Title and message */}
                                                <h4 className="font-semibold text-zinc-900 dark:text-zinc-100">{item.title}</h4>
                                                <p className="text-sm text-zinc-600 dark:text-zinc-400">{item.message}</p>
                                            </div>
                                            {/* Timestamp and action buttons */}
                                            <div className="flex sm:flex-col items-center sm:items-end gap-2">
                                                <span className="text-xs text-zinc-500 whitespace-nowrap">
                                                    {formatTimestamp(item)}
                                                </span>
                                                <div className="flex gap-1">
                                                    <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => handleOpenEdit(item)}>
                                                        <Edit2 className="h-4 w-4 text-zinc-500" />
                                                    </Button>
                                                    <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => handleDelete(item.id)}>
                                                        <Trash2 className="h-4 w-4 text-red-500" />
                                                    </Button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>
            )}

            {/* ═══════════════════════════════════════════════════ */}
            {/* ─── EDIT DIALOG ──────────────────────────────── */}
            {/* ═══════════════════════════════════════════════════ */}
            <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
                <DialogContent className="max-w-lg">
                    <DialogHeader>
                        <DialogTitle>Edit Broadcast</DialogTitle>
                        <DialogDescription>Update the broadcast details below.</DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4 py-2">
                        {/* Title */}
                        <div className="space-y-2">
                            <Label>Title</Label>
                            <Input value={editForm.title} onChange={e => setEditForm({ ...editForm, title: e.target.value })} maxLength={60} />
                        </div>
                        {/* Message */}
                        <div className="space-y-2">
                            <Label>Message</Label>
                            <Textarea value={editForm.message} onChange={e => setEditForm({ ...editForm, message: e.target.value })} maxLength={250} className="min-h-[100px]" />
                        </div>
                        {/* Type and Priority */}
                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label>Type</Label>
                                <Select value={editForm.type} onValueChange={v => setEditForm({ ...editForm, type: v })}>
                                    <SelectTrigger><SelectValue /></SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="INFO">ℹ️ Info</SelectItem>
                                        <SelectItem value="WARNING">⚠️ Warning</SelectItem>
                                        <SelectItem value="ALERT">🚨 Alert</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                            <div className="space-y-2">
                                <Label>Priority</Label>
                                <Select value={editForm.priority} onValueChange={v => setEditForm({ ...editForm, priority: v })}>
                                    <SelectTrigger><SelectValue /></SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="LOW">Low</SelectItem>
                                        <SelectItem value="MEDIUM">Medium</SelectItem>
                                        <SelectItem value="HIGH">High</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>
                        {/* Active toggle */}
                        <div className="flex items-center justify-between border p-3 rounded-md">
                            <Label>Active</Label>
                            <Button
                                variant={editForm.active ? "default" : "outline"}
                                size="sm"
                                onClick={() => setEditForm({ ...editForm, active: !editForm.active })}
                            >
                                {editForm.active ? "Active ✓" : "Inactive"}
                            </Button>
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>Cancel</Button>
                        <Button onClick={handleSaveEdit}>Save Changes</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
