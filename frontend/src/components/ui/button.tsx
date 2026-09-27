import * as React from "react";
import { cn } from "@/lib/utils";
export function Button({ className, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return <button className={cn("button primary", className)} {...props} />;
}
