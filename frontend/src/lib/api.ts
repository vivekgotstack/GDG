import type { Availability, Booking, BookingInput, Member, Room, SearchResult } from "./types";
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      ...init, headers: { "Content-Type": "application/json", ...init?.headers },
      signal: AbortSignal.timeout(175000), cache: "no-store",
    });
  } catch { throw new Error("The meeting service is taking too long to wake up. Please try again shortly."); }
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(error?.detail || error?.message || "Something went wrong. Please try again.");
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
export const api = {
  members: () => request<Member[]>("/members"),
  rooms: () => request<Room[]>("/rooms"),
  bookings: () => request<Booking[]>("/bookings"),
  search: (memberIds: string[], durationMinutes: number, requiredCapacity: number) =>
    request<SearchResult>("/meeting-options/search", { method: "POST", body: JSON.stringify({ memberIds, durationMinutes, requiredCapacity }) }),
  availability: (id: string, availability: Availability[]) =>
    request<Member>(`/members/${id}/availability`, { method: "PUT", body: JSON.stringify({ availability }) }),
  book: (input: BookingInput) => request<Booking>("/bookings", { method: "POST", body: JSON.stringify(input) }),
  reset: () => request<void>("/demo/reset", { method: "POST" }),
};
