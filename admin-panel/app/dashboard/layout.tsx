import ProtectedRoute from "@/components/ProtectedRoute";
import Link from "next/link";
import PageTransition from "@/components/PageTransition";
import { ThemeToggle } from "@/components/theme-toggle";
import { TopNav } from "@/components/top-nav"; // <-- Import the new nav

export default function DashboardLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <ProtectedRoute>
            <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950">
                <header className="sticky top-0 z-50 w-full border-b border-zinc-200 bg-white/80 dark:border-zinc-800 dark:bg-zinc-950/80 backdrop-blur">
                    <div className="container flex h-16 items-center">
                        <Link href="/dashboard" className="text-xl font-bold tracking-tight px-4 border-r border-zinc-200 dark:border-zinc-800 hover:text-blue-600 transition-colors">
                            BusLK Admin Panel
                        </Link>
                        
                        {/* THE NEW NAV IS PLACED HERE */}
                        <TopNav />

                        {/* Theme Button Container (Adjusted to pull the button slightly left) */}
                        <div className="ml-auto pr-6 flex items-center space-x-4">
                            <ThemeToggle />
                            {/* User Profile / Logout button will go here */}
                        </div>

                    </div>
                </header>
                <main className="container mx-auto p-8">
                    <PageTransition>{children}</PageTransition>
                </main>
            </div>
        </ProtectedRoute>
    );
}
