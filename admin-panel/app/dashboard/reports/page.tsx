"use client";

/**
 * ReportsPage — Admin module for viewing Passenger Condition Reports.
 *
 * This page displays crowdsourced alerts flagged by commuters (e.g.,
 * delays, breakdowns, route issues, driver behavior).
 *
 * Features:
 * - Fetches recent reports from the `reports` collection in Firestore.
 * - Resolves raw User IDs to display names via a background lookup map.
 * - Resolves raw Bus IDs to Registration Numbers.
 * - Displays a clean feed of recent alerts.
 */

import { useEffect, useState, useMemo, useCallback } from "react";
import { collection, query, orderBy, limit, getDocs } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { AlertCircle, FileWarning, RefreshCw, Clock, User, Bus } from "lucide-react";
import { toast } from "sonner";
import { formatDistanceToNow } from "date-fns";

// ─── Type Definitions ───────────────────────────────────────

interface ReportData {
    id: string;
    type: string;
    value: string;
    busId: string;
    userId: string;
    description: string;
    timestamp?: any; // Firestore Timestamp
}

interface UserData {
    id: string;
    displayName?: string;
    email?: string;
}

interface BusData {
    id: string;
    registrationNumber: string;
}

// ═══════════════════════════════════════════════════════════
// ─── REPORTS COMPONENT ──────────────────────────────────────
// ═══════════════════════════════════════════════════════════

export default function ReportsPage() {
    // ─── State ──────────────────────────────────────────────
    const [reports, setReports] = useState<ReportData[]>([]);
    const [users, setUsers] = useState<UserData[]>([]);
    const [buses, setBuses] = useState<BusData[]>([]);

    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);

    // ─── Lookup Maps ────────────────────────────────────────

    /** Map user IDs to their display names. Returns ID if name is missing. */
    const userMap = useMemo(() => {
        const map: Record<string, string> = {};
        users.forEach(u => {
            map[u.id] = u.displayName || u.email || "Anonymous Passenger";
        });
        return map;
    }, [users]);

    /** Map bus IDs to their registration numbers. */
    const busMap = useMemo(() => {
        const map: Record<string, string> = {};
        buses.forEach(b => {
            map[b.id] = b.registrationNumber || b.id;
        });
        return map;
    }, [buses]);

    // ─── Data Fetching ──────────────────────────────────────

    /**
     * Fetches reports, users, and buses from Firestore concurrently.
     * Note: If `reports` lacks an index for `timestamp`, `orderBy` will fail.
     * We attempt to orderBy, and fallback to a plain limit query if it fails.
     */
    const fetchData = useCallback(async (showToast = false) => {
        try {
            if (showToast) setRefreshing(true);

            // 1. Fetch auxiliary data for lookups (Users & Buses)
            const [userSnap, busSnap] = await Promise.all([
                getDocs(collection(db, "users")),
                getDocs(collection(db, "buses"))
            ]);

            const userList: UserData[] = [];
            userSnap.forEach(doc => userList.push({ id: doc.id, ...doc.data() } as UserData));
            setUsers(userList);

            const busList: BusData[] = [];
            busSnap.forEach(doc => busList.push({ id: doc.id, ...doc.data() } as BusData));
            setBuses(busList);

            // 2. Fetch Reports (Try ordered, fallback to unordered if index is missing)
            let reportQuery = query(collection(db, "reports"), orderBy("timestamp", "desc"), limit(50));
            let reportSnap;

            try {
                reportSnap = await getDocs(reportQuery);
            } catch (err: any) {
                console.warn("Index for reports orderBy('timestamp') missing. Falling back to unordered query.");
                // Fallback query if the backend lacks the required composite index
                reportQuery = query(collection(db, "reports"), limit(50));
                reportSnap = await getDocs(reportQuery);
            }

            const reportList: ReportData[] = [];
            reportSnap.forEach((doc) => {
                reportList.push({ id: doc.id, ...doc.data() } as ReportData);
            });

            // Client-side sort just in case the fallback query was used
            reportList.sort((a, b) => {
                const timeA = a.timestamp?.toMillis ? a.timestamp.toMillis() : 0;
                const timeB = b.timestamp?.toMillis ? b.timestamp.toMillis() : 0;
                return timeB - timeA;
            });

            setReports(reportList);

            if (showToast) toast.success("Reports refreshed");
        } catch (e: any) {
            console.error("Error fetching reports:", e);
            toast.error("Failed to load generic reports data.");
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, []);

    // Initial load
    useEffect(() => {
        fetchData();
    }, [fetchData]);

    // ─── Helpers ────────────────────────────────────────────

    /** Converts Firestore timestamp to a human-readable relative string */
    const getRelativeTime = (ts: any) => {
        if (!ts) return "Recently";
        try {
            const date = ts.toDate ? ts.toDate() : new Date(ts);
            return formatDistanceToNow(date, { addSuffix: true });
        } catch (e) {
            return "Recently";
        }
    };

    /** Determines badge styling based on report type/value */
    const getReportColor = (value: string | undefined = "") => {
        const val = value.toLowerCase();
        if (val.includes("critical") || val.includes("breakdown") || val.includes("accident")) {
            return "bg-red-50 text-red-700 border-red-200 dark:bg-red-950/50 dark:text-red-400 dark:border-red-900";
        }
        if (val.includes("delay") || val.includes("traffic") || val.includes("crowded")) {
            return "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/50 dark:text-amber-400 dark:border-amber-900";
        }
        return "bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-950/50 dark:text-blue-400 dark:border-blue-900";
    };

    // ═══════════════════════════════════════════════════════════
    // ─── RENDER ─────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════

    return (
        <div className="space-y-6">
            {/* ─── Page Header ───────────────────────────────── */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center bg-white dark:bg-zinc-950 p-6 rounded-xl border shadow-sm gap-4">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">Analytics & Reports</h2>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-1">Review active crowdsourced condition flags by commuters.</p>
                </div>
                <Button variant="outline" size="sm" onClick={() => fetchData(true)} disabled={loading || refreshing}>
                    <RefreshCw className={`h-4 w-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
                    Refresh
                </Button>
            </div>

            <Card className="shadow-sm">
                <CardHeader className="bg-zinc-50 dark:bg-zinc-900/50 border-b border-zinc-100 dark:border-zinc-800">
                    <CardTitle className="flex items-center gap-2">
                        <FileWarning className="h-5 w-5 text-red-500" />
                        Live Network Condition Reports
                    </CardTitle>
                    <CardDescription>
                        {reports.length > 0 ? `Showing ${reports.length} recent alerts flagged across the fleet.` : "Monitoring for network alerts."}
                    </CardDescription>
                </CardHeader>
                <CardContent className="p-0">
                    {loading ? (
                        <div className="p-6 space-y-4">
                            {[1, 2, 3].map((i) => (
                                <Skeleton key={i} className="h-32 w-full rounded-xl" />
                            ))}
                        </div>
                    ) : reports.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-16 px-4 text-center text-zinc-500 dark:text-zinc-400">
                            <div className="h-16 w-16 bg-zinc-100 dark:bg-zinc-900 rounded-full flex items-center justify-center mb-4">
                                <AlertCircle className="h-8 w-8 text-zinc-300 dark:text-zinc-700" />
                            </div>
                            <h3 className="text-lg font-medium text-zinc-900 dark:text-zinc-100 mb-1">No Active Reports</h3>
                            <p className="text-sm max-w-sm">There are currently no active passenger condition reports in the network. All systems clear.</p>
                        </div>
                    ) : (
                        <div className="divide-y divide-zinc-100 dark:divide-zinc-800">
                            {reports.map((report) => (
                                <div key={report.id} className="p-6 hover:bg-zinc-50/50 dark:hover:bg-zinc-900/20 transition-colors">
                                    <div className="flex flex-col sm:flex-row sm:items-start gap-4">

                                        {/* Icon Col */}
                                        <div className="hidden sm:flex shrink-0 h-10 w-10 bg-red-50 dark:bg-red-950/30 rounded-full items-center justify-center border border-red-100 dark:border-red-900/50 mt-1">
                                            <FileWarning className="h-5 w-5 text-red-600 dark:text-red-400" />
                                        </div>

                                        {/* Content Col */}
                                        <div className="flex-1 space-y-3">

                                            {/* Report Title & Badge */}
                                            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                                                <div className="flex items-center gap-2">
                                                    <h4 className="text-base font-semibold text-zinc-900 dark:text-zinc-100">
                                                        {report.type || 'Condition Alert'}
                                                    </h4>
                                                    <Badge variant="outline" className={getReportColor(report.value)}>
                                                        {report.value || 'General'}
                                                    </Badge>
                                                </div>
                                                <div className="flex items-center text-xs text-zinc-500">
                                                    <Clock className="h-3.5 w-3.5 mr-1" />
                                                    {getRelativeTime(report.timestamp)}
                                                </div>
                                            </div>

                                            {/* Report Description */}
                                            <p className="text-sm text-zinc-600 dark:text-zinc-300 leading-relaxed bg-zinc-50 dark:bg-zinc-900/50 p-3 rounded-lg border border-zinc-100 dark:border-zinc-800">
                                                {report.description || "No specific details provided by the passenger."}
                                            </p>

                                            {/* Entities Involved */}
                                            <div className="flex flex-wrap items-center gap-x-6 gap-y-2 pt-2 text-sm text-zinc-500 dark:text-zinc-400">
                                                <div className="flex items-center gap-1.5 focus-within:text-zinc-900 hover:text-zinc-900 dark:hover:text-zinc-200 transition-colors">
                                                    <Bus className="h-4 w-4" />
                                                    <span className="font-medium text-zinc-700 dark:text-zinc-300">
                                                        {busMap[report.busId] || report.busId || 'Unknown Bus'}
                                                    </span>
                                                </div>
                                                <div className="flex items-center gap-1.5 focus-within:text-zinc-900 hover:text-zinc-900 dark:hover:text-zinc-200 transition-colors">
                                                    <User className="h-4 w-4" />
                                                    <span className="font-medium">
                                                        {userMap[report.userId] || report.userId || 'Anonymous'}
                                                    </span>
                                                </div>
                                            </div>

                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}
