import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "MeetGrid — Find your common ground",
  description: "Find the time. Find the room. One search. A thoughtful meeting planner for student teams.",
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
