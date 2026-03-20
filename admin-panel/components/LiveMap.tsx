"use client";

import { useEffect, useState } from "react";
import { rtdb } from "@/lib/firebase";
import { ref, onValue } from "firebase/database";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import L from "leaflet";

// Fix for default marker icons in React Leaflet
const DefaultIcon = L.icon({
    iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
    iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
    shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    tooltipAnchor: [16, -28],
    shadowSize: [41, 41],
});
L.Marker.prototype.options.icon = DefaultIcon;

export default function LiveMap() {
    const [buses, setBuses] = useState<any[]>([]);
    const [mounted, setMounted] = useState(false);
    const colomboCenter = { lat: 6.9271, lng: 79.8612 };

    useEffect(() => { setMounted(true); }, []);

    useEffect(() => {
        const busRef = ref(rtdb, "bus_locations");
        const unsubscribe = onValue(busRef, (snapshot) => {
            if (snapshot.exists()) {
                const data = snapshot.val();
                const locations = Object.keys(data).map(key => ({
                    id: key,
                    ...data[key]
                }));
                setBuses(locations);
            } else {
                setBuses([]);
            }
        });

        return () => unsubscribe();
    }, []);

    if (!mounted) return (
        <div className="h-[600px] w-full rounded-xl border flex items-center justify-center bg-zinc-50 dark:bg-zinc-900">
            <div className="animate-pulse flex flex-col items-center">
                <div className="h-10 w-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4"></div>
                <p className="text-zinc-500 font-medium">Loading map...</p>
            </div>
        </div>
    );

    return (
        <div className="h-[600px] w-full rounded-xl overflow-hidden border shadow-sm">
            <MapContainer
                center={[colomboCenter.lat, colomboCenter.lng]}
                zoom={13}
                scrollWheelZoom={true}
                style={{ height: "100%", width: "100%" }}
            >
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                {buses.map((bus) => (
                    bus.lat && bus.lng && (
                        <Marker key={bus.id} position={[bus.lat, bus.lng]}>
                            <Popup>
                                <div className="font-sans">
                                    <p className="font-bold text-base mb-1">{bus.id}</p>
                                    <p className="text-sm">Route: {bus.routeId || "Unknown"}</p>
                                    <p className="text-sm">Speed: {bus.speed ? `${bus.speed} km/h` : "Stopped"}</p>
                                    <p className="text-sm">Crowd Level: {bus.crowdLevel || "N/A"}</p>
                                    <p className="text-xs text-zinc-500 mt-2">
                                        Last Updated: {bus.lastUpdated ? new Date(bus.lastUpdated * 1000).toLocaleTimeString() : "Just now"}
                                    </p>
                                </div>
                            </Popup>
                        </Marker>
                    )
                ))}
            </MapContainer>
        </div>
    );
}
