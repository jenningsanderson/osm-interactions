package org.heigit.bigspatialdata.oshdb.InteractionsBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.HashSet;

import com.google.common.collect.Sets;

import org.heigit.bigspatialdata.oshdb.api.db.OSHDBH2;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.osm.OSMEntity;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.OSHDBBoundingBox;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTag;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.OSMTag;
import org.heigit.bigspatialdata.oshdb.util.tagtranslator.TagTranslator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

public class InteractionsBuilder {

    public static int NEW_OBJECTS = 0;
    public static int UPDATED_OBJECTS = 0;
    public static int MINOR_VERSION_CHANGE = 0;
    public static int DELETED_OBJECTS = 0;

    public static int count = 0;

    public static HashSet<Integer> minorVersionUIDs = new HashSet<>();
    public static HashSet<Integer> majorVersionUIDs = new HashSet<>();

    public static void main(String[] args) {
        Path h2Path = Paths.get("/Users/jenningsanderson/Desktop/ghana-test.oshdb.mv.db");
//        Path h2Path = Paths.get("/Users/jenningsanderson/Desktop/foss4g_2019_ro_buce.oshdb.mv.db");
        try(OSHDBH2 oshdb = new OSHDBH2(h2Path.toString())){
            TagTranslator tagTranslator = new TagTranslator(oshdb.getConnection());

            Stream<String> result = OSMContributionView.on(oshdb)
                    .areaOfInterest(new OSHDBBoundingBox(-180.0, -90.0, 180.0, 90))
//                    .areaOfInterest(new OSHDBBoundingBox(26.1152, -44.5023, 26.1154, 44.5024))
//                    .areaOfInterest(new OSHDBBoundingBox(-1.763966, -1.609479, 6.298851, 6.298851))
                    .timestamps("2018-01-01T00:00:00Z", "2019-08-01T12:00:00Z")
                    .osmType(OSMType.WAY)
                    .osmTag("building")
                    .groupByEntity()
                    .map(contribs -> {
                        for( OSMContribution contrib : contribs) {
                            int contribUser = contrib.getContributorUserId();
                            OSMEntity before = contrib.getEntityBefore();
                            Geometry beforeGeometry = contrib.getGeometryBefore();
                            OSMEntity after = contrib.getEntityAfter();
                            Geometry afterGeometry = contrib.getGeometryAfter();

                            /*

                                Objective: Identify the type of change that happened here.

                                NOTE: I'm not sure if the list of contributions themselves are guaranteed to be sorted, but also not sure it matters

                            */

                            //If there is no before, it's because this object was just created.
                            if (before == null) {

                                NEW_OBJECTS++;

                                majorVersionUIDs.add(contrib.getContributorUserId());

                            } else if (!after.isVisible()) {

                                DELETED_OBJECTS++;

                                majorVersionUIDs.add(contrib.getContributorUserId());

                                try{
                                    String deleted = deletion(contrib);
                                    System.out.print(deleted);
                                }catch (Exception e) {
                                    System.err.println("DELETION COMPUTATION ERROR ON OBJECT: :" + contrib.getOSHEntity());
                                }

                                //The object was potentially deleted, use the geometry from the previous version and annotate as such.

                            } else if (before.getVersion() == after.getVersion()) {

                                //If the version numbers are the same, then we have a potential minor version
                                try{
                                    String mV = minorVersion(contrib);

                                    System.out.print(mV);

                                } catch (Exception e) {
                                    System.err.println("UNKNOWN ERROR ON OBJECT: :" + contrib.getOSHEntity());
                                }


                            } else {

                                UPDATED_OBJECTS++;

                                majorVersionUIDs.add(contrib.getContributorUserId());

                                //We had a _real_ version change, so what was it?
                                // tag comparison
                                for (OSHDBTag tag : after.getTags()) {
                                    int intKey = tag.getKey();
                                    int intVal = tag.getValue();
                                    // tag only contains ids!
                                    // to get to the strings it self you need to translate
                                    OSMTag translated = tagTranslator.getOSMTagOf(tag);
                                    String strKey = translated.getKey();
                                    String strVal = translated.getValue();
                                }

                                if (contrib.getOSHEntity().getId()==95824315){
                                    System.out.println("Debugging Way: \n"+afterGeometry);
                                }
                            }

//                                before.getTimestamp();
//                                System.out.println("This is an updated object with version: " + after.getVersion());

//                            System.out.println("Before: " + before + " After" + after.getVersion());

//                            System.out.println("");

                        }

                        //need t actually return something here
                        return " ";

                    }).stream();

            result.forEach(s -> {
                //need to actually do something here to call it.
//                System.out.print(s);
                if (count%100000==0){
                    System.err.print("\r"+(count/1000)+"k");
                }
                count++;
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.err.println("\nProcessed " + (count/1000) +  "K contributions");

        System.err.println("\nStatus: \n"+
                "\tNew objects....... "+ NEW_OBJECTS +"\n"+
                "\tUpdated Objects... "+ UPDATED_OBJECTS +"\n"+
                "\tMinor Versions.... "+ MINOR_VERSION_CHANGE +"\n"+
                "\tDeleted Objects... "+ DELETED_OBJECTS);

        System.err.println("========================");
        System.err.println("Minor version users: "+minorVersionUIDs.size());
        System.err.println("Major version users: "+majorVersionUIDs.size());
        System.err.println("\tDifference: " + Sets.difference(minorVersionUIDs,majorVersionUIDs).size());

    }

    /*
        Tracking the specific deletion of an object and the previous geometry.
    */
    public static String deletion(OSMContribution contrib){

        GeoJsonWriter writer = new GeoJsonWriter(18);
        writer.setEncodeCRS(false);

        Geometry beforeGeometry = contrib.getGeometryBefore();

        return "{\"type\":\"Feature\",\"properties\":{"+
                "\"@edit\":\"DELETION\"," +
                "\"@uid\":" + contrib.getContributorUserId() + "," +
                "\"@deleted_uid\":" + contrib.getEntityBefore().getUserId() + "," +
                "\"@id\":" + contrib.getOSHEntity().getId() + "," +
                "\"@validSince\":" + contrib.getEntityBefore().getTimestamp().getRawUnixTimestamp() +"," +
                "\"@validUntil\":" + contrib.getTimestamp().getRawUnixTimestamp() + "}," +
                "\"geometry\":" + writer.write(beforeGeometry) + "}\n";
    }

    public static String minorVersion(OSMContribution contrib){

        Geometry before = contrib.getGeometryBefore();
        Geometry after  = contrib.getGeometryAfter();

        GeoJsonWriter writer = new GeoJsonWriter(18);
                      writer.setEncodeCRS(false);

        if (! before.equals(after) ) {

            //We have a minor geometry change in which a user has changed the nodes.

            MINOR_VERSION_CHANGE++;

            minorVersionUIDs.add(contrib.getContributorUserId());

            // TODO: Eventually this could be turned into a boolean to create smarter objects around these types of edits.
            //       For now, print out geometries we can use to illustrate the point with the appropriate timestamps.
            //  GeometryCollection minorEdit = new GeometryCollection([before, after]);

            return

                        "{\"type\":\"Feature\",\"properties\":{"+
                                "\"@edit\":\"MV_BEFORE\"," +
                                "\"@uid\":" + contrib.getEntityBefore().getUserId() + "," +
                                "\"@id\":" + contrib.getOSHEntity().getId() + "," +
                                "\"@validSince\":" + contrib.getEntityBefore().getTimestamp().getRawUnixTimestamp() + "," +
                                "\"@validUntil\":" + contrib.getTimestamp().getRawUnixTimestamp() + "}," +
                                "\"geometry\":" + writer.write(before) + "}\n" +

                        "{\"type\":\"Feature\",\"properties\":{"+
                                "\"@edit\":\"MV_AFTER\"," +
                                "\"@uid\":" + contrib.getContributorUserId() + "," +
                                "\"@id\":" + contrib.getOSHEntity().getId() + "," +
                                "\"@validSince\":" + contrib.getTimestamp().getRawUnixTimestamp() + "}," +
                                "\"geometry\":" + writer.write(after) + "}\n";

        }else{
            return "";
        }
    }
}