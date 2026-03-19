"use client";

import * as React from "react";
import { Moon, Sun } from "lucide-react";
import { useTheme } from "next-themes";

export function ThemeToggle() {
    const { theme, setTheme, systemTheme } = useTheme();

    const [mounted, setMounted] = React.useState(false);
    React.useEffect(() => setMounted(true), []);

    if (!mounted) {
        return (
            <div className="h-9 w-9 rounded-md border border-zinc-200 bg-white/50 dark:border-zinc-800 dark:bg-zinc-950/50" />
        );
    }

    const currentTheme = theme === "system" ? systemTheme : theme;

    return (
        <button
            onClick={() => setTheme(currentTheme === "light" ? "dark" : "light")}
            className="relative inline-flex h-9 w-9 items-center justify-center rounded-md border border-zinc-200 bg-white/50 dark:border-zinc-800 dark:bg-zinc-950/50 hover:bg-zinc-100 dark:hover:bg-zinc-800 focus:outline-none focus:ring-2 focus:ring-zinc-400 dark:focus:ring-zinc-600 transition-colors"
        >
            <Sun className="h-[1.2rem] w-[1.2rem] rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0 text-zinc-600 dark:text-zinc-400" />
            <Moon className="absolute h-[1.2rem] w-[1.2rem] rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100 text-zinc-600 dark:text-zinc-400" />
            <span className="sr-only">Toggle theme</span>
        </button>
    );
}
