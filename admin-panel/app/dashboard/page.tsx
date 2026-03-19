"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, query, where, limit, orderBy } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
    Users, Bus, MapPin, Navigation, MessageSquare,
    Star, Package, Megaphone, RefreshCcw
} from "lucide-react";
import { Button } from "@/components/ui/button";

interface Stats {
    users: number;
    drivers: number;
    admins: number;
    buses: number;
    routes: number;
    trips: number;
    feedback: number;
    lostFound: number;
    broadcasts: number;
    avgRating: number;
}

export default function DashboardIndex() {
    const [stats, setStats] = useState<Stats>({
        users: 0, drivers: 0, admins: 0, buses: 0, routes: 0,
        trips: 0, feedback: 0, lostFound: 0, broadcasts: 0, avgRating: 0
    });
    const [recentFeedback, setRecentFeedback] = useState<any[]>([]);
    const [recentTrips, setRecentTrips] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    const fetchStats = async () => {
        setLoading(true);
        try {
            // Fetch all collections in parallel
            const [
                usersSnap, busesSnap, routesSnap, tripsSnap,
                feedbackSnap, lostFoundSnap, broadcastsSnap
            ] = await Promise.all([
                getDocs(collection(db, "users")),
                getDocs(collection(db, "buses")),
                getDocs(collection(db, "routes")),
                getDocs(collection(db, "trips")),
                getDocs(collection(db, "feedback")),
                getDocs(collection(db, "lost_and_found")),
                getDocs(collection(db, "broadcasts")),
            ]);

            // Count user roles
            let drivers = 0, admins = 0;
            usersSnap.forEach(doc => {
                const role = doc.data()?.role;
                if (role === "Driver") drivers++;
                if (role === "Admin") admins++;
            });

            // Calculate average rating
            let totalRating = 0, ratingCount = 0;
            const fbList: any[] = [];
            feedbackSnap.forEach(doc => {
                const data = doc.data();
                fbList.push({ id: doc.id, ...data });
                if (data.ratings?.overall) {
                    totalRating += data.ratings.overall;
                    ratingCount++;
                }
            });

            // Sort feedback by timestamp desc and take top 5
            fbList.sort((a, b) => {
                const tA = a.timestamp?.toDate ? a.timestamp.toDate().getTime() : 0;
                const tB = b.timestamp?.toDate ? b.timestamp.toDate().getTime() : 0;
                return tB - tA;
            });
            setRecentFeedback(fbList.slice(0, 5));

            // Trips — sort and take top 5
            const tripList: any[] = [];
            tripsSnap.forEach(doc => tripList.push({ id: doc.id, ...doc.data() }));
            tripList.sort((a, b) => {
                const tA = a.timestamp?.toDate ? a.timestamp.toDate().getTime() : 0;
                const tB = b.timestamp?.toDate ? b.timestamp.toDate().getTime() : 0;
                return tB - tA;
            });
            setRecentTrips(tripList.slice(0, 5));

            setStats({
                users: usersSnap.size,
                drivers,
                admins,
                buses: busesSnap.size,
                routes: routesSnap.size,
                trips: tripsSnap.size,
                feedback: feedbackSnap.size,
                lostFound: lostFoundSnap.size,
                broadcasts: broadcastsSnap.size,
                avgRating: ratingCount > 0 ? Math.round((totalRating / ratingCount) * 10) / 10 : 0
            });
        } catch (error) {
            console.error("Error fetching dashboard stats:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchStats();
    }, []);

    const statCards = [
        { title: "Registered Users", value: stats.users, icon: Users, color: "text-blue-600", bg: "bg-blue-100 dark:bg-blue-900/30", sub: `${stats.drivers} Drivers · ${stats.admins} Admins` },
        { title: "Active Buses", value: stats.buses, icon: Bus, color: "text-green-600", bg: "bg-green-100 dark:bg-green-900/30" },
        { title: "Routes", value: stats.routes, icon: MapPin, color: "text-purple-600", bg: "bg-purple-100 dark:bg-purple-900/30" },
        { title: "Total Trips", value: stats.trips, icon: Navigation, color: "text-orange-600", bg: "bg-orange-100 dark:bg-orange-900/30" },
        { title: "Feedback", value: stats.feedback, icon: MessageSquare, color: "text-cyan-600", bg: "bg-cyan-100 dark:bg-cyan-900/30", sub: `Avg. Rating: ${stats.avgRating}/5` },
        { title: "Lost & Found", value: stats.lostFound, icon: Package, color: "text-rose-600", bg: "bg-rose-100 dark:bg-rose-900/30" },
        { title: "Broadcasts", value: stats.broadcasts, icon: Megaphone, color: "text-amber-600", bg: "bg-amber-100 dark:bg-amber-900/30" },
    ];

    if (loading) {
        return (
            <div className="space-y-6">
                <h2 className="text-3xl font-bold tracking-tight">Overview</h2>
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                    {[1, 2, 3, 4, 5, 6, 7, 8].map(i => (
                        <Card key={i} className="animate-pulse">
                            <CardContent className="pt-6">
                                <div className="h-16 bg-zinc-200 dark:bg-zinc-800 rounded" />
                            </CardContent>
                        </Card>
                    ))}
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-8">
            {/* Header */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">Admin Dashboard</h2>
                    <p className="text-muted-foreground mt-1">
                        Monitor and manage BusLK operations from one central overview.
                    </p>
                </div>
                <Button variant="outline" size="sm" onClick={fetchStats} disabled={loading}>
                    <RefreshCcw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                    Refresh
                </Button>
            </div>

            {/* Stat Cards */}
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                {statCards.map((card) => (
                    <Card key={card.title} className="hover:shadow-md transition-shadow">
                        <CardContent className="pt-6">
                            <div className="flex items-center justify-between">
                                <div className="space-y-1">
                                    <p className="text-sm font-medium text-muted-foreground">{card.title}</p>
                                    <p className="text-3xl font-bold">{card.value}</p>
                                    {card.sub && (
                                        <p className="text-xs text-muted-foreground">{card.sub}</p>
                                    )}
                                </div>
                                <div className={`p-3 rounded-full ${card.bg}`}>
                                    <card.icon className={`h-6 w-6 ${card.color}`} />
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                ))}
            </div>

            {/* Bottom Section: Recent Activity */}
            <div className="grid gap-6 md:grid-cols-2">
                {/* Recent Trips */}
                <Card>
                    <CardHeader className="pb-3">
                        <CardTitle className="flex items-center text-lg">
                            <Navigation className="h-5 w-5 mr-2 text-orange-600" />
                            Recent Trips
                        </CardTitle>
                        <CardDescription>Latest recorded journeys</CardDescription>
                    </CardHeader>
                    <CardContent>
                        {recentTrips.length === 0 ? (
                            <p className="text-sm text-muted-foreground text-center py-4">No trips recorded yet.</p>
                        ) : (
                            <div className="space-y-3">
                                {recentTrips.map((trip) => (
                                    <div key={trip.id} className="flex items-center justify-between p-3 bg-zinc-50 dark:bg-zinc-900/50 rounded-lg border">
                                        <div className="min-w-0">
                                            <p className="text-sm font-medium truncate">
                                                {trip.startLocationName || "?"} → {trip.endLocationName || "?"}
                                            </p>
                                            <p className="text-xs text-muted-foreground">
                                                Route {trip.routeId || "N/A"} · Bus {trip.busId || "N/A"}
                                            </p>
                                        </div>
                                        <Badge variant={trip.status === "COMPLETED" ? "default" : trip.status === "ONGOING" ? "secondary" : "outline"} className="ml-2 shrink-0">
                                            {trip.status || "Unknown"}
                                        </Badge>
                                    </div>
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* Recent Feedback */}
                <Card>
                    <CardHeader className="pb-3">
                        <CardTitle className="flex items-center text-lg">
                            <Star className="h-5 w-5 mr-2 text-yellow-500" />
                            Recent Feedback
                        </CardTitle>
                        <CardDescription>Latest passenger reviews</CardDescription>
                    </CardHeader>
                    <CardContent>
                        {recentFeedback.length === 0 ? (
                            <p className="text-sm text-muted-foreground text-center py-4">No feedback recorded yet.</p>
                        ) : (
                            <div className="space-y-3">
                                {recentFeedback.map((fb) => (
                                    <div key={fb.id} className="p-3 bg-zinc-50 dark:bg-zinc-900/50 rounded-lg border">
                                        <div className="flex items-center justify-between mb-1">
                                            <p className="text-sm font-medium truncate">{fb.userId || "Anonymous"}</p>
                                            <div className="flex items-center ml-2 shrink-0">
                                                <Star className="h-3.5 w-3.5 text-yellow-400 fill-yellow-400 mr-1" />
                                                <span className="text-sm font-semibold">{fb.ratings?.overall || 0}</span>
                                            </div>
                                        </div>
                                        <p className="text-xs text-muted-foreground line-clamp-2 italic">
                                            &quot;{fb.comment || "No comment"}&quot;
                                        </p>
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
