"use client";

import { useState, useEffect, Suspense } from "react";
import { signInWithEmailAndPassword } from "firebase/auth";
import { doc, getDoc } from "firebase/firestore";
import { auth, db } from "@/lib/firebase";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "@/components/ui/card";
import { toast } from "sonner";
import { BusFront, KeyRound } from "lucide-react";

export default function LoginPage() {
    return (
        <Suspense fallback={<div className="min-h-screen flex items-center justify-center bg-zinc-50 dark:bg-zinc-950"><span className="animate-pulse text-zinc-400">Loading...</span></div>}>
            <LoginContent />
        </Suspense>
    );
}

function LoginContent() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const router = useRouter();
    const searchParams = useSearchParams();

    // Show error if redirected from ProtectedRoute
    useEffect(() => {
        if (searchParams.get("error") === "unauthorized") {
            toast.error("Access Denied", { description: "Only users with the Admin role can access this panel." });
        }
    }, [searchParams]);

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        try {
            const cred = await signInWithEmailAndPassword(auth, email, password);

            // Check Firestore for Admin role
            const userDoc = await getDoc(doc(db, "users", cred.user.uid));
            if (userDoc.exists() && userDoc.data()?.role === "Admin") {
                toast.success("Login successful", { description: "Welcome back to the Control Tower." });
                router.push("/dashboard");
            } else {
                toast.error("Access Denied", { description: "Only Admin users can access this panel." });
                await auth.signOut();
            }
        } catch (error: any) {
            toast.error("Login failed", { description: error.message || "An error occurred during login." });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-zinc-50 dark:bg-zinc-950 p-4">
            <div className="absolute inset-0 bg-blue-100/20 dark:bg-blue-900/10 pointer-events-none" />
            <div className="absolute top-0 flex w-full justify-center">
                <div className="h-[2px] w-full bg-gradient-to-r from-transparent via-blue-500 to-transparent opacity-20" />
            </div>

            <div className="z-10 w-full max-w-md">
                <div className="flex justify-center mb-8">
                    <div className="rounded-2xl bg-blue-600 p-3 shadow-lg shadow-blue-500/30">
                        <BusFront className="h-10 w-10 text-white" />
                    </div>
                </div>

                <Card className="border-0 shadow-xl shadow-zinc-200/50 dark:shadow-none bg-white/80 dark:bg-zinc-900/80 backdrop-blur-xl">
                    <CardHeader className="space-y-2 text-center">
                        <CardTitle className="text-3xl font-bold tracking-tight">BusLK Admin</CardTitle>
                        <CardDescription className="text-base">
                            Enter your credentials to access the Control Tower
                        </CardDescription>
                    </CardHeader>
                    <form onSubmit={handleLogin}>
                        <CardContent className="space-y-4">
                            <div className="space-y-2">
                                <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70" htmlFor="email">
                                    Email
                                </label>
                                <Input
                                    id="email"
                                    type="email"
                                    placeholder="admin@buslk.com"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                    className="h-11 bg-white/50 dark:bg-zinc-800/50"
                                />
                            </div>
                            <div className="space-y-2">
                                <div className="flex items-center justify-between">
                                    <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70" htmlFor="password">
                                        Password
                                    </label>
                                </div>
                                <Input
                                    id="password"
                                    type="password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                    className="h-11 bg-white/50 dark:bg-zinc-800/50"
                                />
                            </div>
                        </CardContent>
                        <CardFooter>
                            <Button type="submit" className="w-full h-11 text-base bg-blue-600 hover:bg-blue-700 text-white" disabled={loading}>
                                {loading ? <span className="animate-pulse">Authenticating...</span> : (
                                    <>
                                        <KeyRound className="mr-2 h-4 w-4" />
                                        Sign In
                                    </>
                                )}
                            </Button>
                        </CardFooter>
                    </form>
                </Card>

                <p className="px-8 text-center text-sm text-zinc-500 mt-8">
                    This portal is restricted to authorized Transit Authority personnel only.
                </p>
            </div>
        </div>
    );
}
