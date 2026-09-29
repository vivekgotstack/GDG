package com.meetgrid.config;
import org.springframework.security.core.context.SecurityContextHolder;
public final class WorkspaceIdentity {
 private WorkspaceIdentity() {}
 public static String id() {
   var auth=SecurityContextHolder.getContext().getAuthentication();
   // Legacy is reserved for offline migration and the existing scheduling test fixtures.
   return auth==null || !auth.isAuthenticated() ? "legacy" : auth.getName();
 }
}
