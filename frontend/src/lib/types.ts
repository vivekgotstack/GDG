export const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"] as const;
export type Day = (typeof DAYS)[number];
export type Availability = { dayOfWeek: Day; startTime: string; endTime: string };
export type Member = { id: string; name: string; color: string; availability: Availability[] };
export type MemberInput = Pick<Member, "name" | "color">;
export type Room = { id: string; name: string; capacity: number; openTime: string; closeTime: string; location: string };
export type RoomInput = Omit<Room, "id">;
export type Booking = Availability & { id: string; roomId: string; roomName: string; source: string };
export type BookingInput = Availability & { roomId: string };
export type MeetingOption = Availability & { id: string; availableRooms: Room[]; rejectedRooms: { room: Room; reasons: string[] }[] };
export type SearchResult = { options: MeetingOption[]; commonIntervals: Availability[]; checkedCandidates: number; blockedCandidates: number };
export const dayName = (day: Day) => day.charAt(0) + day.slice(1).toLowerCase();
export const minutes = (time: string) => Number(time.slice(0, 2)) * 60 + Number(time.slice(3, 5));
export function clock(time: string) {
  const hour = Number(time.slice(0, 2));
  return `${hour % 12 || 12}:${time.slice(3, 5)} ${hour >= 12 ? "PM" : "AM"}`;
}
export const range = (start: string, end: string) => `${clock(start)} – ${clock(end)}`;
