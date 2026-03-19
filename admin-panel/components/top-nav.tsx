"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion } from "framer-motion";

const navLinks = [
    { href: "/dashboard/routes", label: "Routes" },
    { href: "/dashboard/fleet", label: "Buses" },
    { href: "/dashboard/qr", label: "QR" },
    { href: "/dashboard/users", label: "Users" },
    { href: "/dashboard/trips", label: "Trips" },
    { href: "/dashboard/map", label: "Map" },
    { href: "/dashboard/broadcasts", label: "Broadcasts" },
    { href: "/dashboard/lost-found", label: "L&F" },
    { href: "/dashboard/feedback", label: "Feedback" },
    { href: "/dashboard/support", label: "Support" },
    { href: "/dashboard/reports", label: "Reports" },
];

export function TopNav() {
    const pathname = usePathname();

    return (
        <nav className="flex items-center space-x-6 text-sm font-medium ml-6 h-16">
            {navLinks.map((link) => {
                // Determine if this is the active route or a sub-route
                const isActive = pathname.startsWith(link.href);
                
                return (
                    <Link
                        key={link.href}
                        href={link.href}
                        className={`relative flex items-center h-full transition-colors hover:text-zinc-900 dark:hover:text-zinc-50 ${
                            isActive 
                                ? "text-zinc-900 dark:text-zinc-50 font-semibold" 
                                : "text-zinc-600 dark:text-zinc-400"
                        }`}
                    >
                        {link.label}
                        
                        {/* Animated Underline */}
                        {isActive && (
                            <motion.span
                                layoutId="nav-underline"
                                className="absolute bottom-[-1px] left-0 right-0 h-[2px] bg-blue-600 dark:bg-blue-500 z-10"
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                exit={{ opacity: 0 }}
                                transition={{ type: "spring", stiffness: 500, damping: 30 }}
                            />
                        )}
                    </Link>
                );
            })}
        </nav>
    );
}
