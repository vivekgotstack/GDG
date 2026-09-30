import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
// One-time, offline recovery utility. H2 is not an application dependency.
class ExportLegacyH2 {
 public static void main(String[] args)throws Exception{
  if(args.length!=2)throw new IllegalArgumentException("Usage: ExportLegacyH2 <legacy database base path> <new output.sql>");
  Class.forName("org.h2.Driver");int total=0;
  try(var db=DriverManager.getConnection("jdbc:h2:file:"+args[0]+";MODE=PostgreSQL;ACCESS_MODE_DATA=r;IFEXISTS=TRUE","sa","");var out=Files.newBufferedWriter(Path.of(args[1]),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){
   out.write("-- Private recovery data. Contains account hashes and workspace records. Do not commit.\n-- Apply to an empty PostgreSQL app database AFTER Flyway migrations, BEFORE admin bootstrap.\nBEGIN;\nSET LOCAL standard_conforming_strings=on;\n");
   for(String table:List.of("accounts","members","rooms","availability","room_bookings","meeting_templates","plan_definitions","site_content","admin_events")){
    try(var st=db.createStatement();var rs=st.executeQuery("SELECT * FROM "+table)){var m=rs.getMetaData();int count=m.getColumnCount();List<String> columns=new ArrayList<>();for(int i=1;i<=count;i++)columns.add(m.getColumnName(i).toLowerCase(Locale.ROOT));
     int rows=0;while(rs.next()){List<String> values=new ArrayList<>();for(int i=1;i<=count;i++){Object v=rs.getObject(i);values.add(v==null?"NULL":v instanceof Number?v.toString():v instanceof Boolean?(Boolean.TRUE.equals(v)?"TRUE":"FALSE"):"'"+rs.getString(i).replace("'","''")+"'");}
      String conflict="";if(table.equals("plan_definitions")||table.equals("site_content"))conflict=" ON CONFLICT(id) DO UPDATE SET "+String.join(",",columns.stream().filter(c->!c.equals("id")).map(c->c+"=EXCLUDED."+c).toList());
      out.write("INSERT INTO "+table+"("+String.join(",",columns)+") VALUES("+String.join(",",values)+")"+conflict+";\n");rows++;total++;}
     System.out.println(table+": "+rows+" rows");}
   }
   out.write("UPDATE accounts SET email_verified=TRUE WHERE role='ADMIN';\nCOMMIT;\n");
  }
  System.out.println("Exported "+total+" records into a private PostgreSQL import file.");
 }
}
