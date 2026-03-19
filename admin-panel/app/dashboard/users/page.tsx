"use client";

import { useEffect, useState } from "react";
import { collection, getDocs, setDoc, updateDoc, deleteDoc, doc, query, where, serverTimestamp } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import { Users, UserCircle2, MapPin, MessageSquare, AlertTriangle, ShieldAlert, Plus, Edit2, Trash2, RefreshCcw } from "lucide-react";
import { format } from "date-fns";

export default function UsersPage() {
    const [users, setUsers] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState("");

    // Selected user state
    const [selectedUser, setSelectedUser] = useState<any | null>(null);
    const [userTrips, setUserTrips] = useState<any[]>([]);
    const [userFeedback, setUserFeedback] = useState<any[]>([]);
    const [userLostFound, setUserLostFound] = useState<any[]>([]);
    const [detailsLoading, setDetailsLoading] = useState(false);

    // CRUD Dialog state
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [editingId, setEditingId] = useState<string | null>(null);
    const [formData, setFormData] = useState({
        uid: "",
        displayName: "",
        email: "",
        phoneNumber: "",
        photoUrl: "",
        role: "Passenger",
        level: "Newcomer",
        points: 0
    });

    // Initial load
    const fetchUsers = async () => {
        try {
            const querySnapshot = await getDocs(collection(db, "users"));
            const userList: any[] = [];
            querySnapshot.forEach((doc) => {
                userList.push({ id: doc.id, ...doc.data() });
            });
            setUsers(userList);

            if (selectedUser) {
                const updated = userList.find(u => u.id === selectedUser.id);
                if (updated) setSelectedUser(updated);
                else setSelectedUser(null);
            }
        } catch (e) {
            console.error(e);
            toast.error("Failed to load users data.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    // Load details when a user is selected
    useEffect(() => {
        if (!selectedUser) {
            setUserTrips([]);
            setUserFeedback([]);
            setUserLostFound([]);
            return;
        }

        const fetchUserDetails = async () => {
            setDetailsLoading(true);
            try {
                // 1. Fetch Trips
                const qTrips = query(collection(db, "trips"), where("userId", "==", selectedUser.uid));
                const tripsSnap = await getDocs(qTrips);
                const tripsData = tripsSnap.docs.map(d => ({ id: d.id, ...d.data() }));
                // Sort client-side if no index is available yet
                tripsData.sort((a: any, b: any) => new Date(b.checkInTime).getTime() - new Date(a.checkInTime).getTime());
                setUserTrips(tripsData);

                // 2. Fetch Feedback
                const qFeedback = query(collection(db, "feedback"), where("userId", "==", selectedUser.uid));
                const feedbackSnap = await getDocs(qFeedback);
                const feedbackData = feedbackSnap.docs.map(d => ({ id: d.id, ...d.data() }));
                feedbackData.sort((a: any, b: any) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
                setUserFeedback(feedbackData);

                // 3. Fetch Lost & Found
                const qLF = query(collection(db, "lost_found"), where("userId", "==", selectedUser.uid));
                const lfSnap = await getDocs(qLF);
                const lfData = lfSnap.docs.map(d => ({ id: d.id, ...d.data() }));
                lfData.sort((a: any, b: any) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
                setUserLostFound(lfData);

            } catch (error) {
                console.error("Error fetching user details:", error);
                toast.error("Failed to load full user history.");
            } finally {
                setDetailsLoading(false);
            }
        };

        fetchUserDetails();
    }, [selectedUser]);


    const filteredUsers = users.filter(u =>
        (u.displayName?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
        (u.email?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
        (u.uid?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
        (u.role?.toLowerCase() || "").includes(searchTerm.toLowerCase())
    );

    const formatDate = (dateString: string | any) => {
        if (!dateString) return 'N/A';
        try {
            // Handle Firestore Timestamp
            if (dateString.toDate) {
                return format(dateString.toDate(), 'PP p');
            }
            return format(new Date(dateString), 'PP p');
        } catch (e) {
            return 'N/A';
        }
    };

    // --- CRUD Handlers ---

    const handleOpenDialog = (userToEdit?: any) => {
        if (userToEdit) {
            setEditingId(userToEdit.id);
            setFormData({
                uid: userToEdit.uid || "",
                displayName: userToEdit.displayName || "",
                email: userToEdit.email || "",
                phoneNumber: userToEdit.phoneNumber || "",
                photoUrl: userToEdit.photoUrl || "",
                role: userToEdit.role || "Passenger",
                level: userToEdit.level || "Newcomer",
                points: userToEdit.points || 0
            });
        } else {
            setEditingId(null);
            const randomUid = Math.random().toString(36).substring(2, 15);
            setFormData({
                uid: `USER_${randomUid.toUpperCase()}`,
                displayName: "",
                email: "",
                phoneNumber: "",
                photoUrl: "",
                role: "Passenger",
                level: "Newcomer",
                points: 0
            });
        }
        setIsDialogOpen(true);
    };

    const handleSaveUser = async () => {
        if (!formData.uid || !formData.displayName || !formData.email) {
            toast.error("UID, Name, and Email are required.");
            return;
        }

        try {
            setIsSubmitting(true);
            const userData = {
                uid: formData.uid,
                displayName: formData.displayName,
                email: formData.email,
                phoneNumber: formData.phoneNumber,
                photoUrl: formData.photoUrl,
                role: formData.role,
                level: formData.level,
                points: Number(formData.points) || 0,
                updatedAt: serverTimestamp(),
            };

            if (editingId) {
                const userRef = doc(db, "users", editingId);
                await updateDoc(userRef, userData);
                toast.success("User updated successfully.");
            } else {
                // Determine doc ID based on UID to prevent duplicates if creating randomly
                const userRef = doc(db, "users", formData.uid);
                await setDoc(userRef, {
                    ...userData,
                    joinedAt: serverTimestamp(),
                    stats: { totalTrips: 0, totalDistanceKm: 0 }
                });
                toast.success("New user record created.");
            }

            setIsDialogOpen(false);
            fetchUsers();
        } catch (error) {
            console.error("Error saving user:", error);
            toast.error("Failed to save user data.");
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleDeleteUser = async (id: string) => {
        if (!confirm("Are you sure you want to delete this user record? This will only remove their profile from the database, not associated trips or their actual Firebase Auth account. It's intended ONLY for sample data cleanup.")) {
            return;
        }

        try {
            await deleteDoc(doc(db, "users", id));
            toast.success("User record deleted.");
            if (selectedUser?.id === id) {
                setSelectedUser(null);
            }
            fetchUsers();
        } catch (error) {
            console.error("Error deleting user:", error);
            toast.error("Failed to delete user.");
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">User Management</h2>
                    <p className="text-muted-foreground mt-1">Review accounts, journey history, and manage sample records.</p>
                </div>
                <div className="flex gap-2">
                    <Button variant="outline" onClick={fetchUsers} disabled={loading}>
                        <RefreshCcw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                        Refresh
                    </Button>
                    <Button onClick={() => handleOpenDialog()}>
                        <Plus className="h-4 w-4 mr-2" />
                        Create Mock User
                    </Button>
                </div>
            </div>

            <div className="grid gap-6 md:grid-cols-12">
                {/* Left Column: User List */}
                <Card className="md:col-span-5 h-[calc(100vh-12rem)] flex flex-col">
                    <CardHeader className="pb-3 flex-shrink-0">
                        <CardTitle>Registered Users</CardTitle>
                        <CardDescription>Browse and search the user base.</CardDescription>
                        <div className="mt-4">
                            <Input
                                placeholder="Search by name, email, or UID..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                        </div>
                    </CardHeader>
                    <CardContent className="overflow-y-auto flex-grow p-0">
                        {loading ? (
                            <div className="p-6 text-center text-sm text-zinc-500">Loading users...</div>
                        ) : filteredUsers.length === 0 ? (
                            <div className="p-6 text-center text-sm text-zinc-500">No users found.</div>
                        ) : (
                            <div className="divide-y divide-zinc-100 dark:divide-zinc-800">
                                {filteredUsers.map((user) => (
                                    <div
                                        key={user.id}
                                        className={`flex items-start justify-between p-4 transition-colors cursor-pointer ${selectedUser?.id === user.id ? 'bg-zinc-100 dark:bg-zinc-800' : 'hover:bg-zinc-50 dark:hover:bg-zinc-900'}`}
                                        onClick={() => setSelectedUser(user)}
                                    >
                                        <div className="flex items-center space-x-4">
                                            {user.photoUrl ? (
                                                <img src={user.photoUrl} alt="avatar" className="h-10 w-10 rounded-full border border-zinc-200 dark:border-zinc-800" referrerPolicy="no-referrer" />
                                            ) : (
                                                <div className="bg-blue-100 p-2 rounded-full dark:bg-blue-900/30 border border-blue-200 dark:border-blue-800">
                                                    <UserCircle2 className="h-6 w-6 text-blue-600 dark:text-blue-400" />
                                                </div>
                                            )}
                                            <div className="overflow-hidden">
                                                <div className="flex items-center gap-2">
                                                    <p className="font-medium truncate">{user.displayName || "Unknown User"}</p>
                                                    <Badge variant={user.role === 'Admin' ? 'destructive' : user.role === 'Driver' ? 'default' : 'secondary'} className="text-[10px] px-1.5 py-0 shrink-0">
                                                        {user.role || 'Passenger'}
                                                    </Badge>
                                                </div>
                                                <p className="text-xs text-zinc-500 truncate">{user.email}</p>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* Right Column: User Details */}
                <div className="md:col-span-7 space-y-6">
                    {!selectedUser ? (
                        <Card className="h-full flex items-center justify-center p-12 text-center text-zinc-500 border-dashed">
                            <div className="space-y-2">
                                <Users className="h-8 w-8 mx-auto opacity-50" />
                                <p>Select a user from the list to view their details, journey history, and feedback.</p>
                            </div>
                        </Card>
                    ) : (
                        <>
                            {/* Identity Card */}
                            <Card>
                                <CardHeader className="flex flex-row items-center justify-between pb-2">
                                    <div className="space-y-1">
                                        <CardTitle className="flex items-center">
                                            {selectedUser.displayName}
                                        </CardTitle>
                                        <CardDescription>{selectedUser.email}</CardDescription>
                                    </div>
                                    <div className="text-right flex flex-col items-end gap-2">
                                        <div className="text-right">
                                            <div className="flex items-center gap-2 justify-end mb-1">
                                                <Badge variant={selectedUser.role === 'Admin' ? 'destructive' : selectedUser.role === 'Driver' ? 'default' : 'secondary'}>
                                                    {selectedUser.role || 'Passenger'}
                                                </Badge>
                                                <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 bg-amber-100/50 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400 border-transparent">
                                                    {selectedUser.level || 'Newcomer'}
                                                </span>
                                            </div>
                                            <p className="text-xs text-zinc-500 mt-1">UID: {selectedUser.uid}</p>
                                        </div>
                                        <div className="flex border-t pt-2 w-full justify-end dark:border-zinc-800 space-x-2">
                                            <Button variant="outline" size="sm" onClick={() => handleOpenDialog(selectedUser)}>
                                                <Edit2 className="h-4 w-4 mr-2" /> Edit
                                            </Button>
                                            <Button variant="outline" size="sm" onClick={() => handleDeleteUser(selectedUser.id)} className="text-red-500 hover:text-red-600 hover:bg-red-50">
                                                <Trash2 className="h-4 w-4 mr-2" /> Delete
                                            </Button>
                                        </div>
                                    </div>
                                </CardHeader>
                                <CardContent className="space-y-4 pt-4">
                                    <div className="grid grid-cols-3 gap-4 text-center">
                                        <div className="border rounded-md p-3 bg-zinc-50 dark:bg-zinc-900/50">
                                            <p className="text-2xl font-bold">{selectedUser.points || 0}</p>
                                            <p className="text-xs text-zinc-500 uppercase tracking-wider font-semibold">Points</p>
                                        </div>
                                        <div className="border rounded-md p-3 bg-zinc-50 dark:bg-zinc-900/50">
                                            <p className="text-2xl font-bold">{selectedUser.stats?.totalTrips || 0}</p>
                                            <p className="text-xs text-zinc-500 uppercase tracking-wider font-semibold">Trips</p>
                                        </div>
                                        <div className="border rounded-md p-3 bg-zinc-50 dark:bg-zinc-900/50">
                                            <p className="text-2xl font-bold">{selectedUser.stats?.totalDistanceKm || 0} <span className="text-sm font-normal">km</span></p>
                                            <p className="text-xs text-zinc-500 uppercase tracking-wider font-semibold">Distance</p>
                                        </div>
                                    </div>
                                    <div className="text-xs text-zinc-500 flex justify-between">
                                        <span>Joined: {formatDate(selectedUser.joinedAt)}</span>
                                        <span>Pref: {selectedUser.preferences?.language?.toUpperCase() || 'EN'} | {selectedUser.preferences?.notificationsEnabled ? '🔔 On' : '🔕 Off'}</span>
                                    </div>
                                </CardContent>
                            </Card>

                            {/* Details Tabs / Content */}
                            {detailsLoading ? (
                                <Card className="p-8 text-center text-zinc-500 text-sm">Loading user history...</Card>
                            ) : (
                                <div className="space-y-6">

                                    {/* Trip History */}
                                    <Card>
                                        <CardHeader className="py-4">
                                            <CardTitle className="text-base flex items-center">
                                                <MapPin className="mr-2 h-4 w-4" /> Journey History
                                            </CardTitle>
                                        </CardHeader>
                                        <CardContent className="p-0 max-h-[300px] overflow-y-auto">
                                            {userTrips.length === 0 ? (
                                                <p className="p-4 text-sm text-zinc-500 text-center">No trips recorded for this user.</p>
                                            ) : (
                                                <div className="divide-y text-sm border-t border-zinc-100 dark:border-zinc-800">
                                                    {userTrips.map(trip => (
                                                        <div key={trip.id} className="p-4 hover:bg-zinc-50 dark:hover:bg-zinc-900/50">
                                                            <div className="flex justify-between font-medium mb-1">
                                                                <span>{trip.startStop || 'Unknown'} ➔ {trip.endStop || 'Unknown'}</span>
                                                                <span className={trip.status === 'COMPLETED' ? 'text-green-600' : 'text-amber-600'}>
                                                                    {trip.status}
                                                                </span>
                                                            </div>
                                                            <div className="flex justify-between text-xs text-zinc-500">
                                                                <span>Bus: {trip.busId} (Route: {trip.routeId})</span>
                                                                <span>{formatDate(trip.checkInTime)}</span>
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            )}
                                        </CardContent>
                                    </Card>

                                    {/* Feedback */}
                                    <Card>
                                        <CardHeader className="py-4">
                                            <CardTitle className="text-base flex items-center">
                                                <MessageSquare className="mr-2 h-4 w-4" /> Submitted Feedback
                                            </CardTitle>
                                        </CardHeader>
                                        <CardContent className="p-0 max-h-[250px] overflow-y-auto">
                                            {userFeedback.length === 0 ? (
                                                <p className="p-4 text-sm text-zinc-500 text-center">No feedback submitted by this user.</p>
                                            ) : (
                                                <div className="divide-y text-sm border-t border-zinc-100 dark:border-zinc-800">
                                                    {userFeedback.map(fb => (
                                                        <div key={fb.id} className="p-4 hover:bg-zinc-50 dark:hover:bg-zinc-900/50">
                                                            <div className="flex items-center space-x-2 mb-2">
                                                                <span className="font-semibold text-amber-600">★ {fb.ratings?.overall || 0}/5</span>
                                                                <span className="text-xs text-zinc-500">Bus: {fb.busId}</span>
                                                                <span className="text-xs text-zinc-400 ml-auto">{formatDate(fb.timestamp)}</span>
                                                            </div>
                                                            <p className="text-zinc-700 dark:text-zinc-300 mb-2">{fb.comment || 'No comment provided.'}</p>
                                                            {fb.tags && fb.tags.length > 0 && (
                                                                <div className="flex gap-1 flex-wrap">
                                                                    {fb.tags.map((tag: string) => (
                                                                        <span key={tag} className="text-[10px] bg-zinc-100 dark:bg-zinc-800 px-2 py-0.5 rounded-full text-zinc-600 dark:text-zinc-400">
                                                                            #{tag}
                                                                        </span>
                                                                    ))}
                                                                </div>
                                                            )}
                                                        </div>
                                                    ))}
                                                </div>
                                            )}
                                        </CardContent>
                                    </Card>

                                    {/* Lost & Found / Reports */}
                                    <Card>
                                        <CardHeader className="py-4">
                                            <CardTitle className="text-base flex items-center">
                                                <ShieldAlert className="mr-2 h-4 w-4" /> Lost & Found Reports
                                            </CardTitle>
                                        </CardHeader>
                                        <CardContent className="p-0 max-h-[250px] overflow-y-auto">
                                            {userLostFound.length === 0 ? (
                                                <p className="p-4 text-sm text-zinc-500 text-center">No items reported by this user.</p>
                                            ) : (
                                                <div className="divide-y text-sm border-t border-zinc-100 dark:border-zinc-800">
                                                    {userLostFound.map(item => (
                                                        <div key={item.id} className="p-4 flex gap-4 hover:bg-zinc-50 dark:hover:bg-zinc-900/50">
                                                            <div className="flex-shrink-0 mt-1">
                                                                <AlertTriangle className={`h-5 w-5 ${item.itemType === 'LOST' ? 'text-red-500' : 'text-blue-500'}`} />
                                                            </div>
                                                            <div className="flex-grow">
                                                                <div className="flex justify-between font-semibold mb-1">
                                                                    <span>[{item.itemType}] {item.title}</span>
                                                                    <span className="text-xs bg-zinc-100 dark:bg-zinc-800 px-2 py-0.5 rounded-full">{item.status}</span>
                                                                </div>
                                                                <p className="text-zinc-600 dark:text-zinc-400 mb-1">{item.description}</p>
                                                                <div className="flex justify-between text-xs text-zinc-500">
                                                                    <span>Bus: {item.busId}</span>
                                                                    <span>{formatDate(item.timestamp)}</span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            )}
                                        </CardContent>
                                    </Card>

                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>

            {/* Create/Edit User Dialog */}
            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>{editingId ? "Edit Mock User Profile" : "Create Mock User Profile"}</DialogTitle>
                        <DialogDescription>
                            Create a standalone record in Firestore to act as a simulated user. This helps visualize data relationships locally without requiring a full Auth registration.
                        </DialogDescription>
                    </DialogHeader>

                    <div className="grid gap-4 py-4">
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label htmlFor="uid" className="text-right">UID</Label>
                            <Input id="uid" value={formData.uid} onChange={(e) => setFormData({ ...formData, uid: e.target.value })} className="col-span-3" placeholder="Auth ID" disabled={!!editingId} />
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label htmlFor="name" className="text-right">Display Name</Label>
                            <Input id="name" value={formData.displayName} onChange={(e) => setFormData({ ...formData, displayName: e.target.value })} className="col-span-3" />
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label htmlFor="email" className="text-right">Email</Label>
                            <Input id="email" type="email" value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} className="col-span-3" />
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label htmlFor="phone" className="text-right">Phone (Opt)</Label>
                            <Input id="phone" value={formData.phoneNumber} onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })} className="col-span-3" />
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                            <Label className="text-right">Role</Label>
                            <div className="col-span-3">
                                <Select value={formData.role} onValueChange={(val) => setFormData({ ...formData, role: val })}>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select a role" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="Passenger">Passenger</SelectItem>
                                        <SelectItem value="Driver">Driver</SelectItem>
                                        <SelectItem value="Admin">Admin</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>

                        <div className="border-t border-zinc-100 dark:border-zinc-800 pt-4 mt-2">
                            <p className="text-sm font-medium mb-4 text-center">Gammification Overrides</p>
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label htmlFor="level" className="text-xs">Reward Level</Label>
                                    <Input id="level" value={formData.level} onChange={(e) => setFormData({ ...formData, level: e.target.value })} />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="points" className="text-xs">Points Balance</Label>
                                    <Input id="points" type="number" value={formData.points} onChange={(e) => setFormData({ ...formData, points: parseInt(e.target.value) || 0 })} />
                                </div>
                            </div>
                        </div>
                    </div>

                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsDialogOpen(false)} disabled={isSubmitting}>Cancel</Button>
                        <Button onClick={handleSaveUser} disabled={isSubmitting}>
                            {isSubmitting ? "Saving..." : "Save Profile"}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
