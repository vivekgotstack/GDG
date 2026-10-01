package com.meetgrid.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetgrid.controller.ReportsController;
import com.meetgrid.controller.ToolsController;
import com.meetgrid.model.*;
import com.meetgrid.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlanEntitlementsTest {
 private final AccountRepository accounts=mock(AccountRepository.class);
 private final MemberRepository members=mock(MemberRepository.class);
 private final RoomRepository rooms=mock(RoomRepository.class);
 private final BookingRepository bookings=mock(BookingRepository.class);
 private final TemplateRepository templates=mock(TemplateRepository.class);
 private final PlanRepository definitions=mock(PlanRepository.class);
 private final PaymentSubscriptionRepository subscriptions=mock(PaymentSubscriptionRepository.class);
 private final PlanService plans=new PlanService(new MockEnvironment(),accounts,members,rooms,bookings,definitions,templates,subscriptions,mock(RazorpayClient.class));
 private Account account;
 private static final String[] TIERS={"free","starter","studio","scale"};
 private static final String[] RESOURCES={"members","rooms","bookings","presets"};
 private static final int[][] CAPS={{5,2,10,3},{10,3,30,5},{30,10,150,25},{100,30,600,100}};
 @BeforeEach void setup(){
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("owner","",List.of()));
  account=new Account();account.id="owner";
  when(accounts.findById("owner")).thenReturn(Optional.of(account));when(accounts.lockById("owner")).thenReturn(Optional.of(account));
  for(int i=1;i<TIERS.length;i++){var p=new PlanDefinition();p.id=TIERS[i];p.name=List.of("Preview","Gather","Studio","Collective").get(i);p.currency="INR";p.description="Test plan";p.members=CAPS[i][0];p.rooms=CAPS[i][1];p.bookings=CAPS[i][2];p.presets=CAPS[i][3];when(definitions.findById(p.id)).thenReturn(Optional.of(p));}
 }
 @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
 private void count(String resource,long value){switch(resource){case "members"->when(members.countByOwnerId("owner")).thenReturn(value);case "rooms"->when(rooms.countByOwnerId("owner")).thenReturn(value);case "presets"->when(templates.countByOwnerId("owner")).thenReturn(value);default->when(bookings.countByRoomOwnerId("owner")).thenReturn(value);}}
 @TestFactory Stream<DynamicTest> allTiersEnforceEveryCapacityBoundary(){
  var cases=new ArrayList<DynamicTest>();for(int t=0;t<TIERS.length;t++)for(int r=0;r<RESOURCES.length;r++){int tier=t,resource=r;cases.add(DynamicTest.dynamicTest(TIERS[t]+" / "+RESOURCES[r],()->{
   account.plan=TIERS[tier];String kind=RESOURCES[resource];int cap=CAPS[tier][resource];count(kind,cap-1);assertThatCode(()->plans.requireCapacity(kind,1)).doesNotThrowAnyException();count(kind,cap);assertThatThrownBy(()->plans.requireCapacity(kind,1)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
  }));}return cases.stream();
 }
 @Test void paidToolsAreDistinctAndInherited(){
  for(int i=0;i<TIERS.length;i++){account.plan=TIERS[i];int tier=i;String[] features={"bulk_import","insights","operations_reports"};assertThat(plans.entitlements().features()).hasSize(i);
   for(int f=0;f<features.length;f++){String feature=features[f];if(f<tier)assertThatCode(()->plans.requireFeature(feature)).doesNotThrowAnyException();else assertThatThrownBy(()->plans.requireFeature(feature)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");}
  }
 }
 @Test void confirmedPaidThroughDateControlsAccessEvenWithStaleAccountTier(){
  account.plan="scale";account.subscriptionId="sub_owned";var sub=new PaymentSubscription();sub.id="sub_owned";sub.ownerId="owner";sub.planId="studio";sub.status="cancelled";sub.paidUntil=Instant.now().plusSeconds(3600);when(subscriptions.findById(sub.id)).thenReturn(Optional.of(sub));
  assertThat(plans.entitlements().planId()).isEqualTo("studio");assertThatCode(()->plans.requireFeature("insights")).doesNotThrowAnyException();assertThatThrownBy(()->plans.requireFeature("operations_reports")).isInstanceOf(ResponseStatusException.class);
  sub.paidUntil=Instant.now().minusSeconds(1);assertThat(plans.entitlements().planId()).isEqualTo("free");assertThatThrownBy(()->plans.requireFeature("bulk_import")).isInstanceOf(ResponseStatusException.class);
  sub.paidUntil=null;sub.status="authenticated";assertThat(plans.entitlements().features()).isEmpty();
  sub.paidUntil=Instant.now().plusSeconds(3600);sub.ownerId="someone_else";assertThat(plans.entitlements().features()).isEmpty();
  when(subscriptions.findById(sub.id)).thenReturn(Optional.empty());assertThat(plans.entitlements().features()).isEmpty();
 }
 @Test void adminHasAllToolsAndNoCapsButRegularUsersCannotBypass(){
  account.role="ADMIN";assertThat(plans.entitlements().features()).hasSize(3);count("members",1000);assertThatCode(()->plans.requireCapacity("members",100)).doesNotThrowAnyException();
  account.role="USER";assertThat(plans.entitlements().features()).isEmpty();assertThatThrownBy(()->plans.requireCapacity("members",1)).isInstanceOf(ResponseStatusException.class);
 }
 @Test void bulkImportChecksFullBatchBeforeAnyWrites(){
  account.plan="starter";when(members.findByOwnerId("owner")).thenReturn(List.of());count("members",9);
  var tools=new ToolsController(members,rooms,bookings,templates,accounts,plans);
  assertThatThrownBy(()->tools.importMembers(new ToolsController.Import(List.of("New one","New two")))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");verify(members,never()).saveAll(any());
  account.plan="free";assertThatThrownBy(()->tools.importMembers(new ToolsController.Import(List.of("New one")))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");verify(members,never()).saveAll(any());
 }
 @Test void reportsGuardDirectApiCallsAndScopeQueriesToCurrentOwner(){
  var reports=new ReportsController(plans,rooms,bookings);
  assertThatThrownBy(reports::insights).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");assertThatThrownBy(reports::operations).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");verifyNoInteractions(rooms,bookings);
  account.plan="studio";when(rooms.findByOwnerId("owner")).thenReturn(List.of());when(bookings.findByRoomOwnerId("owner")).thenReturn(List.of());assertThat(reports.insights().days()).isEmpty();verify(rooms).findByOwnerId("owner");verify(bookings).findByRoomOwnerId("owner");assertThatThrownBy(reports::operations).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
  account.plan="scale";assertThatCode(reports::operations).doesNotThrowAnyException();
 }
 @Test void publicPlanJsonIncludesExactlyTheToolsEnforcedByBackend(){
  var json=new ObjectMapper();for(var plan:plans.all()){var node=json.valueToTree(plan);assertThat(node.has("features")).isTrue();var values=new HashSet<String>();node.path("features").forEach(item->values.add(item.asText()));assertThat(values).isEqualTo(PlanService.featuresFor(plan.id()));}
 }
}
