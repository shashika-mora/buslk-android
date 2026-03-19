"use client";

import { useState, useEffect, useRef, useMemo } from "react";
import { collection, query, orderBy, limit, getDocs, doc, onSnapshot, addDoc, serverTimestamp, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Search, Send, User, MessageCircle, AlertCircle, Clock, CheckCircle2, RefreshCcw } from "lucide-react";
import { toast } from "sonner";
import { format } from "date-fns";

// ─── Type Definitions ───────────────────────────────────────

interface SupportThread {
    id: string;
    userId: string;
    subject?: string;
    status?: string;
    lastUpdatedAt?: any; // Firestore Timestamp
    createdAt?: any;
    lastMessagePreview?: string;
    hasUnreadAdmin?: boolean;
    hasUnreadUser?: boolean;
}

interface Message {
    id: string;
    text: string;
    sender: string;
    timestamp?: any;
}

interface UserData {
    id: string;
    displayName?: string;
    email?: string;
}

// ═══════════════════════════════════════════════════════════
// ─── SUPPORT COMPONENT ──────────────────────────────────────
// ═══════════════════════════════════════════════════════════

export default function SupportPage() {
    // ─── State ──────────────────────────────────────────────
    const [threads, setThreads] = useState<SupportThread[]>([]);
    const [filteredThreads, setFilteredThreads] = useState<SupportThread[]>([]);
    const [selectedThread, setSelectedThread] = useState<SupportThread | null>(null);
    const [messages, setMessages] = useState<Message[]>([]);
    const [users, setUsers] = useState<UserData[]>([]);

    const [replyMessage, setReplyMessage] = useState("");
    const [searchTerm, setSearchTerm] = useState("");
    const [loadingThreads, setLoadingThreads] = useState(true);
    const [isSending, setIsSending] = useState(false);

    const messagesEndRef = useRef<HTMLDivElement>(null);

    // ─── Lookup Maps ────────────────────────────────────────

    /** Map user IDs to their display names or emails. */
    const userMap = useMemo(() => {
        const map: Record<string, string> = {};
        users.forEach(u => {
            map[u.id] = u.displayName || u.email || "Passenger";
        });
        return map;
    }, [users]);

    // ─── Data Fetching ──────────────────────────────────────

    // 0. Fetch User Metadata (for resolving IDs to Names)
    useEffect(() => {
        const fetchUsers = async () => {
            try {
                const userSnap = await getDocs(collection(db, "users"));
                const userList: UserData[] = [];
                userSnap.forEach(doc => userList.push({ id: doc.id, ...doc.data() } as UserData));
                setUsers(userList);
            } catch (err) {
                console.error("Error fetching users for support inbox:", err);
            }
        };
        fetchUsers();
    }, []);

    // 1. Fetch active threads (users who reached out for support)
    useEffect(() => {
        const fetchThreads = async () => {
            try {
                setLoadingThreads(true);
                // We're querying a hypothetical 'support_threads' collection. 
                // If it doesn't exist, this will just return empty.
                const q = query(
                    collection(db, "support_threads"),
                    orderBy("lastUpdatedAt", "desc"),
                    limit(50)
                );

                // Using snapshot for real-time updates on threads (new messages, status changes)
                const unsubscribe = onSnapshot(q, (snapshot) => {
                    const fetched: any[] = [];
                    snapshot.forEach((doc) => {
                        fetched.push({ id: doc.id, ...doc.data() });
                    });
                    setThreads(fetched);
                    setFilteredThreads(fetched);
                    setLoadingThreads(false);
                }, (error) => {
                    console.error("Error listening to threads:", error);
                    // Fallback to one-time fetch without ordering if index is missing
                    getFallbackThreads();
                });

                return () => unsubscribe();
            } catch (error) {
                console.error("Error setting up thread listener:", error);
                getFallbackThreads();
            }
        };

        fetchThreads();
    }, []);

    const getFallbackThreads = async () => {
        try {
            const q = query(collection(db, "support_threads"), limit(50));
            const querySnapshot = await getDocs(q);
            const fetched: any[] = [];
            querySnapshot.forEach((doc) => {
                fetched.push({ id: doc.id, ...doc.data() });
            });
            // Client side sort
            fetched.sort((a, b) => {
                const timeA = a.lastUpdatedAt ? new Date(a.lastUpdatedAt).getTime() : 0;
                const timeB = b.lastUpdatedAt ? new Date(b.lastUpdatedAt).getTime() : 0;
                return timeB - timeA;
            });
            setThreads(fetched);
            setFilteredThreads(fetched);
            setLoadingThreads(false);
        } catch (e) {
            setLoadingThreads(false);
            // Non-intrusive logging, as the collection might not exist yet
            console.log("No support threads found or missing collection.");
        }
    }

    // 2. Filter threads locally
    useEffect(() => {
        if (!searchTerm) {
            setFilteredThreads(threads);
            return;
        }
        const lower = searchTerm.toLowerCase();
        const filtered = threads.filter(t => {
            const userName = userMap[t.userId]?.toLowerCase() || "";
            return (
                (t.userId && t.userId.toLowerCase().includes(lower)) ||
                (t.subject && t.subject.toLowerCase().includes(lower)) ||
                (t.id && t.id.toLowerCase().includes(lower)) ||
                userName.includes(lower)
            );
        });
        setFilteredThreads(filtered);
    }, [searchTerm, threads]);

    // 3. Fetch messages when a thread is selected
    useEffect(() => {
        if (!selectedThread) {
            setMessages([]);
            return;
        }

        const q = query(
            collection(db, `support_threads/${selectedThread.id}/messages`),
            orderBy("timestamp", "asc")
        );

        const unsubscribe = onSnapshot(q, (snapshot) => {
            const fetchedMsg: any[] = [];
            snapshot.forEach((doc) => {
                fetchedMsg.push({ id: doc.id, ...doc.data() });
            });
            setMessages(fetchedMsg);
            scrollToBottom();

            // If the thread was unread by admin, mark it as read upon opening
            if (selectedThread.hasUnreadAdmin) {
                updateDoc(doc(db, "support_threads", selectedThread.id), {
                    hasUnreadAdmin: false
                }).catch(err => console.error("Could not mark thread as read", err));
            }

        }, (error) => {
            console.error("Error fetching messages:", error);
            toast.error("Could not load messages. Please check database permissions.");
        });

        return () => unsubscribe();
    }, [selectedThread?.id]);

    const scrollToBottom = () => {
        setTimeout(() => {
            messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
        }, 100);
    };

    const handleSendMessage = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!replyMessage.trim() || !selectedThread) return;

        try {
            setIsSending(true);
            const msgData = {
                text: replyMessage.trim(),
                sender: "admin", // Identifier for admin portal
                timestamp: serverTimestamp(),
            };

            // Add message to subcollection
            await addDoc(collection(db, `support_threads/${selectedThread.id}/messages`), msgData);

            // Update parent thread metadata
            await updateDoc(doc(db, "support_threads", selectedThread.id), {
                lastUpdatedAt: serverTimestamp(),
                lastMessagePreview: replyMessage.trim().substring(0, 50),
                hasUnreadUser: true, // Mark so the user app shows a badge
                status: "active"
            });

            setReplyMessage("");
            scrollToBottom();
        } catch (error: any) {
            console.error("Error sending message:", error);
            toast.error("Failed to send reply. " + error.message);
        } finally {
            setIsSending(false);
        }
    };

    const handleResolveThread = async () => {
        if (!selectedThread) return;
        try {
            await updateDoc(doc(db, "support_threads", selectedThread.id), {
                status: "resolved",
                lastUpdatedAt: serverTimestamp()
            });
            toast.success("Thread marked as resolved.");
            // UI will update automatically due to onSnapshot
        } catch (error) {
            toast.error("Failed to resolve thread.");
        }
    };

    const formatThreadTime = (ts: any) => {
        if (!ts) return "";
        const date = typeof ts.toDate === 'function' ? ts.toDate() : new Date(ts);
        if (isNaN(date.getTime())) return "";

        const now = new Date();
        const isToday = date.getDate() === now.getDate() && date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear();

        return isToday ? format(date, 'h:mm a') : format(date, 'MMM d');
    };

    return (
        <div className="space-y-6 max-w-7xl mx-auto h-[calc(100vh-120px)] flex flex-col">
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center space-y-4 md:space-y-0 flex-shrink-0">
                <div>
                    <h2 className="text-3xl font-bold tracking-tight">Support Inbox</h2>
                    <p className="text-muted-foreground mt-1">Manage passenger inquiries and provide assistance.</p>
                </div>
                <Button variant="outline" size="sm" onClick={() => window.location.reload()}>
                    <RefreshCcw className="h-4 w-4 mr-2" />
                    Refresh
                </Button>
            </div>

            <div className="flex-1 grid grid-cols-1 md:grid-cols-3 gap-6 overflow-hidden">
                {/* Left Panel: Thread List */}
                <Card className="md:col-span-1 flex flex-col overflow-hidden border-zinc-200 dark:border-zinc-800">
                    <div className="p-4 border-b border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/50">
                        <div className="relative">
                            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-zinc-500" />
                            <Input
                                type="search"
                                placeholder="Search queries..."
                                className="pl-8 bg-white dark:bg-zinc-950"
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                            />
                        </div>
                    </div>

                    <ScrollArea className="flex-1">
                        {loadingThreads ? (
                            <div className="p-8 text-center text-sm text-zinc-500">Loading inbox...</div>
                        ) : filteredThreads.length === 0 ? (
                            <div className="p-8 text-center flex flex-col items-center text-zinc-500">
                                <MessageCircle className="h-8 w-8 mb-2 opacity-20" />
                                <p className="text-sm">No active support threads.</p>
                            </div>
                        ) : (
                            <div className="flex flex-col">
                                {filteredThreads.map((thread) => (
                                    <button
                                        key={thread.id}
                                        onClick={() => setSelectedThread(thread)}
                                        className={`p-4 text-left border-b border-zinc-100 dark:border-zinc-800 hover:bg-zinc-50 dark:hover:bg-zinc-900/50 transition-colors ${selectedThread?.id === thread.id ? 'bg-zinc-100 dark:bg-zinc-800/50' : ''}`}
                                    >
                                        <div className="flex justify-between items-start mb-1">
                                            <span className={`text-sm font-medium truncate pr-2 ${thread.hasUnreadAdmin ? 'text-zinc-900 dark:text-zinc-50 font-bold' : 'text-zinc-700 dark:text-zinc-300'}`}>
                                                {userMap[thread.userId] || thread.userId || "Passenger"}
                                            </span>
                                            <span className="text-[10px] text-zinc-500 whitespace-nowrap pt-0.5">
                                                {formatThreadTime(thread.lastUpdatedAt || thread.createdAt)}
                                            </span>
                                        </div>
                                        <div className="flex items-center space-x-2">
                                            {thread.status === 'resolved' ? (
                                                <Badge variant="outline" className="text-[10px] px-1 py-0 h-4 bg-green-50 text-green-700 border-green-200 dark:bg-green-900/20 dark:text-green-400 dark:border-green-900/50">Resolved</Badge>
                                            ) : thread.hasUnreadAdmin ? (
                                                <Badge variant="default" className="text-[10px] px-1 py-0 h-4 bg-blue-600">New</Badge>
                                            ) : (
                                                <div className="h-4 w-1 flex-shrink-0" /> // spacer
                                            )}
                                            <span className="text-xs text-zinc-500 truncate">{thread.subject || thread.lastMessagePreview || "No messages yet."}</span>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        )}
                    </ScrollArea>
                </Card>

                {/* Right Panel: Chat Interface */}
                <Card className="md:col-span-2 flex flex-col overflow-hidden border-zinc-200 dark:border-zinc-800">
                    {selectedThread ? (
                        <>
                            {/* Chat Header */}
                            <div className="p-4 border-b border-zinc-200 dark:border-zinc-800 flex justify-between items-center bg-zinc-50 dark:bg-zinc-900/50 shrink-0">
                                <div>
                                    <h3 className="font-semibold text-zinc-900 dark:text-zinc-50">
                                        Ticket: {selectedThread.subject || "General Inquiry"}
                                    </h3>
                                    <div className="flex items-center text-xs text-zinc-500 mt-1">
                                        <User className="h-3 w-3 mr-1" /> {userMap[selectedThread.userId] || selectedThread.userId || "Passenger"}
                                        <span className="mx-2">•</span>
                                        <span className="font-mono">{selectedThread.id}</span>
                                    </div>
                                </div>
                                {selectedThread.status !== 'resolved' && (
                                    <Button variant="outline" size="sm" onClick={handleResolveThread}>
                                        <CheckCircle2 className="h-4 w-4 mr-2 text-green-600 dark:text-green-500" />
                                        Mark Resolved
                                    </Button>
                                )}
                            </div>

                            {/* Chat Messages */}
                            <ScrollArea className="flex-1 p-4 bg-white dark:bg-zinc-950">
                                {messages.length === 0 ? (
                                    <div className="h-full flex flex-col items-center justify-center text-zinc-500 space-y-2 py-12">
                                        <MessageCircle className="h-8 w-8 opacity-20" />
                                        <p className="text-sm">No messages in this thread yet.</p>
                                    </div>
                                ) : (
                                    <div className="space-y-4">
                                        {messages.map((msg, idx) => {
                                            const isAdmin = msg.sender === 'admin';
                                            return (
                                                <div key={msg.id || idx} className={`flex flex-col ${isAdmin ? 'items-end' : 'items-start'}`}>
                                                    <div
                                                        className={`max-w-[80%] rounded-2xl px-4 py-2 text-sm ${isAdmin
                                                            ? 'bg-blue-600 text-white rounded-br-sm'
                                                            : 'bg-zinc-100 text-zinc-900 dark:bg-zinc-800 dark:text-zinc-100 rounded-bl-sm'
                                                            }`}
                                                    >
                                                        {msg.text}
                                                    </div>
                                                    <span className="text-[10px] text-zinc-400 mt-1 px-1">
                                                        {formatThreadTime(msg.timestamp)}
                                                        {isAdmin && " • Admin"}
                                                    </span>
                                                </div>
                                            );
                                        })}
                                        <div ref={messagesEndRef} />
                                    </div>
                                )}
                            </ScrollArea>

                            {/* Chat Input */}
                            <div className="p-4 border-t border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/50 shrink-0">
                                {selectedThread.status === 'resolved' ? (
                                    <div className="text-center p-3 text-sm text-zinc-500 border border-dashed border-zinc-300 dark:border-zinc-700 rounded-md">
                                        This ticket has been marked as resolved.
                                    </div>
                                ) : (
                                    <form onSubmit={handleSendMessage} className="flex space-x-2">
                                        <Input
                                            placeholder="Type your reply here..."
                                            value={replyMessage}
                                            onChange={(e) => setReplyMessage(e.target.value)}
                                            className="flex-1 bg-white dark:bg-zinc-950"
                                            disabled={isSending}
                                        />
                                        <Button type="submit" disabled={isSending || !replyMessage.trim()}>
                                            <Send className="h-4 w-4" />
                                            <span className="sr-only">Send</span>
                                        </Button>
                                    </form>
                                )}
                            </div>
                        </>
                    ) : (
                        <div className="flex flex-col items-center justify-center h-full text-zinc-500 p-8 text-center bg-zinc-50/50 dark:bg-zinc-900/20">
                            <MessageCircle className="h-12 w-12 mb-4 opacity-20" />
                            <h3 className="text-lg font-medium text-zinc-900 dark:text-zinc-100 mb-1">No Thread Selected</h3>
                            <p className="text-sm max-w-sm">Select a support ticket from the list on the left to view the conversation and reply to the passenger.</p>
                        </div>
                    )}
                </Card>
            </div>
        </div>
    );
}
