import type { Availability, Booking, BookingInput, Member, MemberInput, Room, RoomInput, SearchResult } from "./types";
export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const csrfHeaders: Record<string,string> = {};
  if (init?.method && !['GET','HEAD'].includes(init.method)) {
    const csrfResponse = await fetch('/api/auth/csrf', {cache:'no-store',credentials:'same-origin'});
    if (!csrfResponse.ok) throw new Error('Could not secure this request. Please try again.');
    const csrf = await csrfResponse.json(); csrfHeaders[csrf.headerName] = csrf.token;
  }
  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      ...init, credentials:'same-origin', headers: { "Content-Type": "application/json", ...csrfHeaders, ...init?.headers },
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
  saveMember: (input: MemberInput, id?: string) => request<Member>(id ? `/members/${encodeURIComponent(id)}` : "/members", { method: id ? "PUT" : "POST", body: JSON.stringify(input) }),
  deleteMember: (id: string) => request<void>(`/members/${encodeURIComponent(id)}`, { method: "DELETE" }),
  saveRoom: (input: RoomInput, id?: string) => request<Room>(id ? `/rooms/${encodeURIComponent(id)}` : "/rooms", { method: id ? "PUT" : "POST", body: JSON.stringify(input) }),
  deleteRoom: (id: string) => request<void>(`/rooms/${encodeURIComponent(id)}`, { method: "DELETE" }),
  cancelBooking: (id: string) => request<void>(`/bookings/${encodeURIComponent(id)}`, { method: "DELETE" }),
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
