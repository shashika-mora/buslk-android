"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, setDoc, deleteDoc, doc, query, where } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { toast } from "sonner";
import { Plus, Trash2, Map, Pencil, Search, RefreshCcw } from "lucide-react";

export default function RoutesPage() {
    const [routes, setRoutes] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState("");

    // Form State
    const [routeId, setRouteId] = useState("");
    const [routeName, setRouteName] = useState("");
    const [startLocation, setStartLocation] = useState("");
    const [endLocation, setEndLocation] = useState("");
    const [distanceKm, setDistanceKm] = useState("");

    const [selectedRoute, setSelectedRoute] = useState<any | null>(null);
    const [isEditing, setIsEditing] = useState(false);
    const [routeBuses, setRouteBuses] = useState<any[]>([]);
    const [loadingBuses, setLoadingBuses] = useState(false);

    const handleEditClick = (route: any) => {
        setRouteId(route.routeId || route.id);
        setRouteName(route.name || "");
        setStartLocation(route.startLocation || "");
        setEndLocation(route.endLocation || "");
        setDistanceKm(route.distanceKm?.toString() || "");
        setIsEditing(true);
    };

    const handleCancelEdit = () => {
        setIsEditing(false);
        setRouteId("");
        setRouteName("");
        setStartLocation("");
        setEndLocation("");
        setDistanceKm("");
        setSelectedRoute(null);
    };

    const fetchRoutes = async () => {
        try {
            const querySnapshot = await getDocs(collection(db, "routes"));
            const routeList: any[] = [];
            querySnapshot.forEach((doc) => {
                routeList.push({ id: doc.id, ...doc.data() });
            });
            setRoutes(routeList);
        } catch (e) {
            console.error(e);
            toast.error("Failed to load routes data.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchRoutes();
    }, []);

    useEffect(() => {
        if (!selectedRoute) {
            setRouteBuses([]);
            return;
        }
        const fetchBusesForRoute = async () => {
            setLoadingBuses(true);
            try {
                const q = query(collection(db, "buses"), where("defaultRouteId", "==", selectedRoute.routeId));
                const querySnapshot = await getDocs(q);
                const buses: any[] = [];
                querySnapshot.forEach((doc) => {
                    buses.push({ id: doc.id, ...doc.data() });
                });
                setRouteBuses(buses);
            } catch (error) {
                console.error("Error fetching route buses:", error);
                toast.error("Failed to load buses for this route.");
            } finally {
                setLoadingBuses(false);
            }
        };
        fetchBusesForRoute();
    }, [selectedRoute]);

    const handleAddRoute = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!routeId || !routeName || !startLocation || !endLocation) {
            toast.error("Please fill in all required fields.");
            return;
        }

        try {
            const routeData: any = {
                routeId: routeId,
                name: routeName,
                startLocation: startLocation,
                endLocation: endLocation,
                distanceKm: parseFloat(distanceKm) || 0,
            };

            await setDoc(doc(db, "routes", routeId), routeData, { merge: true });

            toast.success(isEditing ? "Route updated successfully" : "Route added successfully");
            handleCancelEdit();
            fetchRoutes();
        } catch (e: any) {
            toast.error(isEditing ? "Error updating route" : "Error adding route", { description: e.message });
        }
    };

    const handleDeleteRoute = async (id: string) => {
        try {
            await deleteDoc(doc(db, "routes", id));
            toast.success("Route deleted");
            if (selectedRoute?.id === id) {
                handleCancelEdit();
            }
            fetchRoutes();
        } catch (e: any) {
            toast.error("Error deleting route", { description: e.message });
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <h2 className="text-3xl font-bold tracking-tight">Route Management</h2>
                <Button variant="outline" size="sm" onClick={fetchRoutes} disabled={loading}>
                    <RefreshCcw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                    Refresh
                </Button>
            </div>

            <div className="grid gap-6 md:grid-cols-2">
                <div className="space-y-6">
                    <Card>
                        <CardHeader>
                            <CardTitle>{isEditing ? 'Edit Route Details' : 'Add New Route'}</CardTitle>
                            <CardDescription>{isEditing ? 'Update the details of the selected route.' : 'Define a new path in the transit network.'}</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <form onSubmit={handleAddRoute} className="space-y-4">
                                <div className="space-y-2">
                                    <label className="text-sm font-medium">Route ID (Number)</label>
                                    <Input
                                        placeholder="e.g. 138"
                                        value={routeId}
                                        onChange={(e) => setRouteId(e.target.value)}
                                        required
                                        disabled={isEditing}
                                    />
                                </div>
                                <div className="space-y-2">
                                    <label className="text-sm font-medium">Route Name</label>
                                    <Input
                                        placeholder="e.g. Colombo - Homagama"
                                        value={routeName}
                                        onChange={(e) => setRouteName(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <label className="text-sm font-medium">Start Location</label>
                                        <Input
                                            placeholder="e.g. Pettah"
                                            value={startLocation}
                                            onChange={(e) => setStartLocation(e.target.value)}
                                            required
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <label className="text-sm font-medium">End Location</label>
                                        <Input
                                            placeholder="e.g. Homagama"
                                            value={endLocation}
                                            onChange={(e) => setEndLocation(e.target.value)}
                                            required
                                        />
                                    </div>
                                </div>
                                <div className="space-y-2">
                                    <label className="text-sm font-medium">Distance (km)</label>
                                    <Input
                                        type="number"
                                        step="0.1"
                                        placeholder="e.g. 24.5"
                                        value={distanceKm}
                                        onChange={(e) => setDistanceKm(e.target.value)}
                                    />
                                </div>
                                <div className="flex space-x-2">
                                    <Button type="submit" className="flex-1">
                                        {isEditing ? (
                                            <>Update Route</>
                                        ) : (
                                            <><Plus className="mr-2 h-4 w-4" /> Add Route</>
                                        )}
                                    </Button>
                                    {isEditing && (
                                        <Button type="button" variant="outline" onClick={handleCancelEdit}>
                                            Cancel
                                        </Button>
                                    )}
                                </div>
                            </form>
                        </CardContent>
                    </Card>

                    {selectedRoute && (
                        <Card>
                            <CardHeader className="flex flex-row items-center justify-between pb-2">
                                <div className="space-y-1">
                                    <CardTitle>Route Details</CardTitle>
                                    <CardDescription>Information for Route {selectedRoute.routeId}</CardDescription>
                                </div>
                                <div className="space-x-2">
                                    <Button variant="outline" size="sm" onClick={() => handleEditClick(selectedRoute)}>
                                        <Pencil className="mr-2 h-4 w-4" /> Edit
                                    </Button>
                                    <Button variant="outline" size="sm" onClick={() => setSelectedRoute(null)}>Close</Button>
                                </div>
                            </CardHeader>
                            <CardContent className="space-y-3 text-sm">
                                <div className="grid grid-cols-2 gap-2">
                                    <span className="font-semibold text-zinc-500">Route ID:</span>
                                    <span>{selectedRoute.routeId}</span>

                                    <span className="font-semibold text-zinc-500">Name:</span>
                                    <span>{selectedRoute.name}</span>

                                    <span className="font-semibold text-zinc-500">Path:</span>
                                    <span>{selectedRoute.startLocation} ➔ {selectedRoute.endLocation}</span>

                                    <span className="font-semibold text-zinc-500">Distance:</span>
                                    <span>{selectedRoute.distanceKm ? `${selectedRoute.distanceKm} km` : 'N/A'}</span>
                                </div>

                                <div className="mt-4 border-t pt-4">
                                    <h4 className="font-semibold mb-2">Buses on this route</h4>
                                    {loadingBuses ? (
                                        <p className="text-sm text-zinc-500">Loading buses...</p>
                                    ) : routeBuses.length === 0 ? (
                                        <p className="text-sm text-zinc-500">No buses assigned to this route.</p>
                                    ) : (
                                        <ul className="space-y-2">
                                            {routeBuses.map((bus) => (
                                                <li key={bus.id} className="text-sm flex flex-col p-3 bg-zinc-50 dark:bg-zinc-900/50 rounded-md border">
                                                    <div className="flex justify-between items-center mb-1">
                                                        <span className="font-medium text-zinc-900 dark:text-zinc-100">{bus.registrationNumber}</span>
                                                        <span className="text-xs bg-zinc-200 dark:bg-zinc-800 px-2 py-0.5 rounded-full text-zinc-700 dark:text-zinc-300">
                                                            {bus.type || 'Bus'}
                                                        </span>
                                                    </div>
                                                    <div className="flex justify-between text-xs text-zinc-500">
                                                        <span>{bus.owner || 'Unknown Owner'}</span>
                                                        <span>{bus.capacity ? `${bus.capacity} seats` : ''}</span>
                                                    </div>
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                </div>
                            </CardContent>
                        </Card>
                    )}
                </div>

                <Card>
                    <CardHeader>
                        <CardTitle>Active Routes</CardTitle>
                        <CardDescription>Manage defined transit paths.</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="relative mb-4">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-500" />
                            <Input
                                placeholder="Search by route ID, name, or location..."
                                className="pl-10"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                            />
                        </div>
                        {loading ? (
                            <p className="text-sm text-zinc-500">Loading routes data...</p>
                        ) : routes.length === 0 ? (
                            <p className="text-sm text-zinc-500">No routes found in the database.</p>
                        ) : (
                            <div className="space-y-4">
                                {routes.filter(route => {
                                    if (!searchQuery) return true;
                                    const q = searchQuery.toLowerCase();
                                    return (
                                        (route.routeId || route.id || "").toLowerCase().includes(q) ||
                                        (route.name || "").toLowerCase().includes(q) ||
                                        (route.startLocation || "").toLowerCase().includes(q) ||
                                        (route.endLocation || "").toLowerCase().includes(q)
                                    );
                                }).map((route) => (
                                    <div
                                        key={route.id}
                                        className={`flex items-center justify-between p-4 border rounded-lg transition-colors cursor-pointer ${selectedRoute?.id === route.id ? 'bg-zinc-100 border-zinc-300 dark:bg-zinc-800 dark:border-zinc-700' : 'hover:bg-zinc-50 dark:border-zinc-800 dark:hover:bg-zinc-900'}`}
                                        onClick={() => setSelectedRoute(route)}
                                    >
                                        <div className="flex items-center space-x-4">
                                            <div className="bg-green-100 p-2 rounded-full dark:bg-green-900/30">
                                                <Map className="h-5 w-5 text-green-600 dark:text-green-400" />
                                            </div>
                                            <div>
                                                <p className="font-semibold">{route.routeId} - {route.name}</p>
                                                <p className="text-sm text-zinc-500">{route.startLocation} to {route.endLocation}</p>
                                            </div>
                                        </div>
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30 z-10"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleDeleteRoute(route.id);
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
