# BusLK Admin Panel

![Next JS](https://img.shields.io/badge/Next-black?style=for-the-badge&logo=next.js&logoColor=white)
![React](https://img.shields.io/badge/react-%2320232a.svg?style=for-the-badge&logo=react&logoColor=%2361DAFB)
![TailwindCSS](https://img.shields.io/badge/tailwindcss-%2338B2AC.svg?style=for-the-badge&logo=tailwind-css&logoColor=white)
![Firebase](https://img.shields.io/badge/firebase-%23039BE5.svg?style=for-the-badge&logo=firebase)
![TypeScript](https://img.shields.io/badge/typescript-%23007ACC.svg?style=for-the-badge&logo=typescript&logoColor=white)

The **BusLK Admin Panel** is a comprehensive web-based dashboard built for administrators to manage, monitor, and configure the BusLK public transit network infrastructure. It provides real-time oversight of fleet operations, user management, analytics, and direct support communication.

## 🚀 Tech Stack

- **Framework**: [Next.js](https://nextjs.org/) (React, App Router)
- **Styling**: [Tailwind CSS](https://tailwindcss.com/)
- **UI Components**: [shadcn/ui](https://ui.shadcn.com/)
- **Icons**: [Lucide React](https://lucide.dev/)
- **Database & Sync**: Firebase (Cloud Firestore & Realtime Database)
- **Authentication**: Firebase Authentication
- **Mapping**: React-Leaflet (OpenStreetMap)
- **QR Codes**: qrcode.react

## ✨ Core Modules & Features

1. **Overview Dashboard**: High-level metrics encompassing total active users, fleet size, route counts, and daily completed trips.
2. **Live Network Monitoring (Map)**: Real-time visual tracking of active buses using coordinates synced through the Firebase Realtime Database.
3. **Fleet Management**: Full CRD (Create, Read, Delete) management for physical buses, linking them to default routes and registration numbers.
4. **QR Code Generator**: Generates mobile-scannable QR codes for check-in/check-out interactions linked directly to active physical buses.
5. **Route Configuration**: Configure start points, endpoints, and identifiers for the network routes.
6. **User Management**: Inspect registered passengers and delve into granular details like their trip history, submitted feedback, and lost & found reports.
7. **Support Inbox**: A live split-pane chat moderation view directly linking administrators to users facing issues.
8. **Broadcasts Module**: Send push alerts (Information, Warnings, Alerts) either network-wide or targeting specific commutters on a route.
9. **Trip Ledger**: Comprehensive system-wide financial and logistical logs of completed passenger journeys.
10. **Feedback & Analytics**: Review anonymised commuter satisfaction metrics, including star ratings for cleanliness, comfort, and driver behavior.

## 🛠️ Local Development Setup

To run the admin panel locally, you need [Node.js](https://nodejs.org/) installed along with a corresponding Firebase project environment.

1. **Install Dependencies**
   ```bash
   npm install
   # or
   yarn install
   # or
   pnpm install
   ```

2. **Environment Variables**
   Ensure your `.env.local` file contains your Firebase project configurations:
   ```env
   NEXT_PUBLIC_FIREBASE_API_KEY=""
   NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=""
   NEXT_PUBLIC_FIREBASE_PROJECT_ID=""
   NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=""
   NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=""
   NEXT_PUBLIC_FIREBASE_APP_ID=""
   NEXT_PUBLIC_FIREBASE_DATABASE_URL=""
   ```

3. **Start the Development Server**
   ```bash
   npm run dev
   # or
   yarn dev
   # or
   pnpm dev
   ```

4. **Access the App**
   Open your browser and navigate to [http://localhost:3000](http://localhost:3000)

## 🔐 Authentication

The application is protected by a standard login barrier.
*Note: In production environments, Ensure Firebase Authentication uses Custom Claims (`admin: true`) to secure these routes beyond standard credentials.*
