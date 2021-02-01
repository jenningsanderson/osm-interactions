import static org.locationtech.jts.algorithm.Angle.angleBetween;
import static org.locationtech.jts.algorithm.Angle.toDegrees;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBH2;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.OSMContributionView;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.osm.OSMEntity;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTag;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTagKey;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;

public class InteractionsBuilder {


    public static long NEW_OBJECTS = 0;
    public static long UPDATED_OBJECTS = 0;
    public static long MINOR_VERSION_CHANGE = 0;
    public static int  DELETED_OBJECTS = 0;
    public static long MAJOR_GEOMETRY_CHANGE = 0;

    public static int DELETION_ERRORS = 0;
    public static int CREATION_ERRORS = 0;
    public static int MAV_ERRORS = 0;
    public static int TAG_ERRORS = 0;
    public static int MIN_VERSION_ERRORS = 0;
    public static int EMPTY_CONTRIBS = 0;
    public static int ENTITY_FAILS = 0;

    public static boolean PRINT = true;

    public static long count = 0;

    public static final boolean LOG_ERRORS   = true;
    public static final boolean PRINT_ERRORS = true;

//    public static HashSet<Integer> mVUIDs = new HashSet<>();
//    public static HashSet<Integer> majorVersionUIDs = new HashSet<>();
    
    // get case insensitive tag-keys as integer set for easy checking
    private static Set<Integer> getCaseInsensitiveKey(AdvTagTranslator tagTranslator, String key) throws SQLException{
      final boolean caseSensitive = false;
      
      final Set<OSHDBTagKey> keys = tagTranslator.getOSHDBTagKeyOf(key,caseSensitive);
      final Set<Integer> intKeys = new HashSet<>(keys.size());
      for( OSHDBTagKey k : keys) {
        intKeys.add(k.toInt());
      }
      return intKeys;
    }
        
    public static void main(String[] args) {

        /* Point to where the configuration file is */
        String configFilePath = "~/data/interaction-extraction.config";

        /* Initialize the variables to parse */
        String h2GridPath = null;
        String h2KeytablesPath = null;
        Polygon areaOfInterest = null;
        GeoJsonReader interestedAreaReader = new GeoJsonReader();
        FileWriter statusFileWriter = null;
        PrintWriter statusPrintWriter = null;

        GeoJsonWriter writer = new GeoJsonWriter(18);

        /* Resolve home directory short hand*/
        if (configFilePath.startsWith("~" + File.separator)) {
            configFilePath = System.getProperty("user.home") + configFilePath.substring(1);
        }

        try {
            String configJSONString = new String( Files.readAllBytes(Paths.get(configFilePath)) );
            JSONObject config = JSON.parseObject(configJSONString);

            h2GridPath = config.get("oshdb").toString();
            h2KeytablesPath = config.get("keytables").toString();
            System.err.println("Will use oshdb: " + h2GridPath.toString());

            Geometry geom = interestedAreaReader.read(config.get("bounds").toString());
            areaOfInterest = geom.getFactory().createPolygon(geom.getCoordinates());

            if (areaOfInterest.isValid()) {
                System.err.println("Found Valid Geometry for bounds: ");
                System.err.println(areaOfInterest.toString());
            } else {
                throw new Error("Bounding area is not valid");
            }

            /* Open extraction status file */
            statusFileWriter = new FileWriter("extraction.status");
            statusPrintWriter = new PrintWriter((statusFileWriter));



            statusPrintWriter.println("Extraction Started at: " + LocalDateTime.now());
            statusPrintWriter.println("---------------------------------------------");
            statusPrintWriter.println(writer.write(areaOfInterest));
            statusPrintWriter.println("===============================");
            statusPrintWriter.flush();

        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }


        try (var oshdb = new OSHDBH2(h2GridPath);
            var keytables = new OSHDBH2(h2KeytablesPath);
            var tagTranslator = new AdvTagTranslator(keytables.getConnection())){

            // prefetch filter tagKey sets
            final Set<Integer> tagKeyAdminLevel =  getCaseInsensitiveKey(tagTranslator,"admin_level");
            final Set<Integer> tagKeyNatural = getCaseInsensitiveKey(tagTranslator,"natural");
            final Set<Integer> tagKeyRoute = getCaseInsensitiveKey(tagTranslator,"route");
            final Set<Integer> tagKeyBoundary = getCaseInsensitiveKey(tagTranslator,"boundary");
            final Set<Integer> tagKeyType = getCaseInsensitiveKey(tagTranslator,"type");

            final Set<Integer> tagKeyBuilding = getCaseInsensitiveKey(tagTranslator,"building");
            final Set<Integer> tagKeyLandUse = getCaseInsensitiveKey(tagTranslator,"landuse");
            final Set<Integer> tagKeyRestriction = getCaseInsensitiveKey(tagTranslator,"restriction");
            
            //Turn on parallelization
            PrintWriter finalStatusPrintWriter = statusPrintWriter;
            Stream<Integer> result = OSMContributionView.on(oshdb.multithreading(true)).keytables(keytables)

//            Stream<Integer> result = OSMContributionView.on(oshdb) //For easier debugging

//                    // TODO: read GeoJSON bounds?
//                    .areaOfInterest(new OSHDBBoundingBox(83.951151,28.181861, 84.024995, 28.241129)) // Pokhara, Nepal
//                    .areaOfInterest(new OSHDBBoundingBox(83.9769,28.2122478921, 83.9805663895, 28.2146488456)) // Tiny Pokhara

//                   .areaOfInterest(new OSHDBBoundingBox(-180.0,13, -50, 90)) // North America

//                     .areaOfInterest(new OSHDBBoundingBox(-95,-60, -30, 13)) // South America

                      //: Tristan Da Cunha//westlimit=-13.0229; southlimit=-37.6789; eastlimit=-11.9177; northlimit=-36.5501

//                    .areaOfInterest(new OSHDBBoundingBox(-13,-38, -11, -37)) // South America

//                    .areaOfInterest(new Polygonal(P))

                      .areaOfInterest(areaOfInterest)

//                    .areaOfInterest(new OSHDBBoundingBox(-25.6,-55, 180, 37.8)) // Africa, Australia, SE Asia

                    /* Could use any of the following filters: */
//                    .timestamps("2005-04-25T00:00:00Z", "2008-01-01T00:00:00Z")
//                    .osmType(OSMType.RELATION)
//                    .osmTag("highway")
                    
                    // use osmEntityFilter for filter out as early as possible unwanted entities
                    .osmEntityFilter(osm -> {

//                        if(osm.getId() == 193450525){
//                            return true;
//                        }else{
//                            return false;
//                        }
//                      Relations are special...
                        if( osm.getType().equals(OSMType.RELATION) ){
                            for( OSHDBTag tag : osm.getTags()) {

                                /* If it's a restriction or a building, keep it */
                                if (tagKeyRestriction.contains(tag.getKey()) || tagKeyBuilding.contains(tag.getKey()) ){
                                    return true;
                                }

                                /* Keeping landuse tags for now */
                                if (tagKeyLandUse.contains(tag.getKey()) ) {
                                    return true;
                                }
                            }
                            return false;
                        }else{

                            /* Also ignore objects that have these attributes: */
                            for( OSHDBTag tag : osm.getTags()) {

                                /* Skip all boundaries & routes */
                                if (tagKeyBoundary.contains(tag.getKey()) || tagKeyRoute.contains(tag.getKey()) ){
                                    return false;
                                }

                                /* Skip natural objects... including trees, lakes, etc. */
                                if(tagKeyNatural.contains(tag.getKey())) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    })
                    .groupByEntity()
                    .map(contribs -> {

                        //Iterates through the contributions, sorted by timestamp...ASC?;
                        int idx = 0;
                        int minorVersionValue = 0;
                        long oneLaterContribTime = 0;
                        boolean checkMinorVersion = true;

                        try {
                            for (OSMContribution contrib : contribs) {

                                String contribPropertyString = "";                     // This becomes the final output
                                String geometryString = getGeometryString(contrib);    // The geometry (can be "null")

                                OSMEntity before = contrib.getEntityBefore();
                                OSMEntity after  = contrib.getEntityAfter();

                                /* if it's the first entry, set some values: */
                                if (idx==0){
                                    Map<String, String> curTags = tagTranslator.getTagsAsKeyValueMap(after.getTags());

                                    if (after.getType() == OSMType.RELATION && (curTags.containsKey("restriction")) ){
                                        checkMinorVersion = false;
                                    }
                                }

                                //If there is another contribution after this one, get that value.
                                if (idx == contribs.size() - 1) {
                                    oneLaterContribTime = 0;
                                } else if (contribs.size() > (idx + 1)) {
                                    oneLaterContribTime = contribs.get(idx + 1).getTimestamp().getRawUnixTimestamp();
                                }

                                /* If it's the latest version of this object, then set the tags */
                                if (oneLaterContribTime == 0) {
                                    Map<String, String> curTags = tagTranslator.getTagsAsKeyValueMap(after.getTags());
                                    String tagString = JSON.toJSONString((curTags));
                                    //If it has current tags, set the property string:
                                    if (!curTags.isEmpty()) {
                                        contribPropertyString = tagString.substring(1, tagString.length() - 1) + ",";
                                    }
                                }

                                /* If it's a new object */
                                if (contrib.getContributionTypes().contains(ContributionType.CREATION)) {
                                    NEW_OBJECTS++;

                                    try {
                                        contribPropertyString += creation(contrib);

                                        //Get the tags
                                        Map<String, String> createdTags = tagTranslator.getTagsAsKeyValueMap(contrib.getEntityAfter().getTags());

                                        if (!createdTags.isEmpty()) {
                                            contribPropertyString += "\"@aA\":" + JSON.toJSONString(createdTags) + ",";
                                        }

                                    } catch (Exception e) {
                                        CREATION_ERRORS++;
                                        if (PRINT_ERRORS) {
                                            System.err.println("CRE: " +
                                                    contrib.getOSHEntity().getType().toString() + " | " + contrib.getOSHEntity().getId());
                                        }
                                        if (LOG_ERRORS){
                                            finalStatusPrintWriter.println("CRE: " +
                                                    contrib.getOSHEntity().getType().toString() + " | " + contrib.getOSHEntity().getId());
                                            e.printStackTrace(finalStatusPrintWriter);
                                        }

                                    }

                                } else if (contrib.getContributionTypes().contains(ContributionType.DELETION)) {
                                    DELETED_OBJECTS++;

                                    try {
                                        contribPropertyString = deletion(contrib);

                                        Map<String, String> deletedTags = tagTranslator.getTagsAsKeyValueMap(contrib.getEntityBefore().getTags());

                                        if (!deletedTags.isEmpty()) {
                                            contribPropertyString += "\"@aD\":" + JSON.toJSONString(deletedTags) + ",";
                                        }

                                        /* This edit is valid for 1 second */
                                        oneLaterContribTime = contrib.getTimestamp().getRawUnixTimestamp() + 1;

                                    } catch (Exception e) {
                                        DELETION_ERRORS++;
                                        if (PRINT_ERRORS) {
                                            System.err.println("DEL: " +
                                                    contrib.getOSHEntity().getType().toString() + " | " + contrib.getOSHEntity().getId());
                                        }

                                        if (LOG_ERRORS) {
                                            finalStatusPrintWriter.println("DEL: " +
                                                    contrib.getOSHEntity().getType().toString() + " | " + contrib.getOSHEntity().getId());
                                            e.printStackTrace(finalStatusPrintWriter);
                                        }
                                    }

                                } else if (before.getVersion() == after.getVersion()) {

                                    //If the version numbers are the same, then we have a potential minor version
                                    try {
                                        if (checkMinorVersion && !beforeGeometryEqualsAfterGeometry(contrib)) {
                                            minorVersionValue++;
                                            contribPropertyString += minorVersion(contrib, minorVersionValue);
                                        }

                                    } catch (Exception e) {
                                        MIN_VERSION_ERRORS++;
                                        if (PRINT_ERRORS) {
                                            System.err.println("MIN-VER: " +
                                                    contrib.getOSHEntity().getType().toString() + " | " +
                                                    contrib.getOSHEntity().getId() + " | " +
                                                    before.getVersion());
                                        }
                                        if (LOG_ERRORS){
                                            finalStatusPrintWriter.println("MIN-VER: " +
                                                    contrib.getOSHEntity().getType().toString() + " | " + contrib.getOSHEntity().getId());
                                            e.printStackTrace(finalStatusPrintWriter);
                                        }
                                    }

                                } else {

                                    //It's a major version, so reset the mV counter; also, note the mV counter only works if we're starting at t0
                                    minorVersionValue = 0;

                                    // These are visible updates to the object that are visible on the map with versioning
                                    UPDATED_OBJECTS++;

                                    if (contrib.getContributionTypes().contains(ContributionType.TAG_CHANGE)) {

                                        try {
                                            // TODO: A tag change, what are some major tag changes we care about?
                                            Map<String, String> beforeTags = tagTranslator.getTagsAsKeyValueMap(before.getTags());
                                            Map<String, String> afterTags = tagTranslator.getTagsAsKeyValueMap(after.getTags());

                                            Map<String, String> newTags;
                                            Map<String, String[]> modTags = new HashMap<>();
                                            Map<String, String> delTags;

                                            MapDifference<String, String> difference = Maps.difference(beforeTags, afterTags);

                                            newTags = difference.entriesOnlyOnRight();
                                            Map<String, ValueDifference<String>> diffTags = difference.entriesDiffering();
                                            diffTags.forEach((strKey, diff) -> {
                                                String[] mod = {diff.leftValue(), diff.rightValue()};
                                                modTags.put(strKey, mod);
                                            });
                                            delTags = difference.entriesOnlyOnLeft();

                                            if (!newTags.isEmpty()) {
                                                contribPropertyString += "\"@aA\":" + JSON.toJSONString(newTags) + ",";
                                            }
                                            if (!modTags.isEmpty()) {
                                                contribPropertyString += "\"@aM\":" + JSON.toJSONString(modTags) + ",";
                                            }
                                            if (!delTags.isEmpty()) {
                                                contribPropertyString += "\"@aD\":" + JSON.toJSONString(delTags) + ",";
                                            }
                                        } catch (Exception e) {
                                            TAG_ERRORS++;
                                            if (PRINT_ERRORS) {
                                                System.err.println("TAG: " +
                                                        contrib.getOSHEntity().getType().toString() + " | " +
                                                        contrib.getOSHEntity().getId() + " | " +
                                                        after.getVersion());
                                            }

                                            if (LOG_ERRORS) {
                                                finalStatusPrintWriter.println("TAG: " +
                                                        contrib.getOSHEntity().getType().toString() + " | " + contrib.getOSHEntity().getId());
                                                e.printStackTrace(finalStatusPrintWriter);
                                            }
                                        }
                                    }

                                    try {

                                        /* If the geometries are equal, then it's just a MAV */
                                        if (beforeGeometryEqualsAfterGeometry(contrib)) {
                                            contribPropertyString += "\"@e\":\"MAV\",";

                                            /* if the geometries are not equal, then it's a MAG */
                                        } else {
                                            contribPropertyString += majorGeometry(contrib);
                                        }

                                    } catch (Exception e) {
                                        MAV_ERRORS++;
                                        if (PRINT_ERRORS) {
                                            System.err.println("ERR: MAJOR GEOMETRY FAIL: " +
                                                    contrib.getOSHEntity().getType().toString() + " | " +
                                                    contrib.getOSHEntity().getId() + " | " +
                                                    after.getVersion());
                                        }

                                        if (LOG_ERRORS) {
                                            finalStatusPrintWriter.println("ERR: MAJOR GEOMETRY FAIL: " +
                                                    contrib.getOSHEntity().getType().toString() + " | " + contrib.getOSHEntity().getId());
                                            e.printStackTrace(finalStatusPrintWriter);
                                        }
                                    }
                                }

                                if (PRINT) {
                                    if (!contribPropertyString.equals("")) {

                                        System.out.println("{\"type\":\"Feature\",\"properties\":{" +

                                                //add properties here
                                                contribPropertyString +

                                                "\"@vS\":" + contrib.getTimestamp().getRawUnixTimestamp() + "," +
                                                "\"@vU\":" + ((oneLaterContribTime == 0) ? null : oneLaterContribTime) + "," +
                                                "\"@v\":"  + contrib.getEntityAfter().getVersion() + "," +
                                                "\"@uid\":" + contrib.getContributorUserId() + "," +
                                                "\"@id\":" + contrib.getOSHEntity().getId() + "," +
                                                "\"@c\":" + contrib.getChangesetId() +
                                                "}," +
                                                "\"geometry\":" + geometryString + "}");
                                    } else {
                                        EMPTY_CONTRIBS++;
//                                        System.err.println("{\"type\":\"Feature\",\"properties\":{" +
//                                                contribPropertyString +
//
//                                                        "\"@vS\":" + contrib.getTimestamp().getRawUnixTimestamp() + "," +
//                                                        "\"@vU\":" + ((oneLaterContribTime == 0) ? null : oneLaterContribTime) + "," +
//                                                        "\"@v\":"  + contrib.getEntityAfter().getVersion() + "," +
//                                                        "\"@uid\":" + contrib.getContributorUserId() + "," +
//                                                        "\"@id\":" + contrib.getOSHEntity().getId() + "," +
//                                                        "\"@c\":" + contrib.getChangesetId() +
//                                                        "}," +
//                                                        "\"geometry\":" + geometryString + "}");
                                    }
                                }

                                idx++;
                            }
                        }catch(Exception anyError){
                            ENTITY_FAILS++;
                            if (LOG_ERRORS){
                                finalStatusPrintWriter.println("ENTIRE ENTITY?");
                                anyError.printStackTrace(finalStatusPrintWriter);
                            }
                        }

                        // Return the number of interactions processed.
                        return idx;
                    }).stream();

            result.forEach(s -> {
                //need to actually do something here to call it.
                if (count%10000==0){
                    System.err.print("\r"+(count/1000)+"k");

                    if (count%100000==0) {
                        finalStatusPrintWriter.println(count / 1000000.0 + "M interactions" +
                                " |CRE:" + CREATION_ERRORS + " |DEL:" + DELETION_ERRORS + " |MIN:" + MIN_VERSION_ERRORS +
                                " |MAV:" + MAV_ERRORS + " |TAG:" + TAG_ERRORS + " |EMPTY:" + EMPTY_CONTRIBS + " |ENTITY:"+ENTITY_FAILS);
                        finalStatusPrintWriter.flush();
                    }
                }
                count += s;

            });

        } catch (Exception e) {
            e.printStackTrace();
            statusPrintWriter.println("ERR=====\n");
            statusPrintWriter.println(e.toString());
            statusPrintWriter.println("\n=====ERR");
            statusPrintWriter.flush();
        }

        statusPrintWriter.println("\nProcessed " + (count) +  " interactions total:");
        statusPrintWriter.println("|CRE:" + CREATION_ERRORS + " |DEL:" + DELETION_ERRORS + " |MIN:" + MIN_VERSION_ERRORS +
                " |MAV:" + MAV_ERRORS + " |TAG:" + TAG_ERRORS + " |EMPTY:" + EMPTY_CONTRIBS + " |ENTITY:"+ENTITY_FAILS);
        statusPrintWriter.println("Status: \n"+
                "\tNew objects....... "+ NEW_OBJECTS +"\n"+
                "\tUpdated Objects... "+ UPDATED_OBJECTS +"\n"+
                "\tMajor Geometries.. "+ MAJOR_GEOMETRY_CHANGE +"\n"+
                "\tMinor Versions.... "+ MINOR_VERSION_CHANGE +"\n"+
                "\tDeleted Objects... "+ DELETED_OBJECTS + "\n" +
                "\tFailed Entities... "+ ENTITY_FAILS);
        statusPrintWriter.println("-------------------------------");
        statusPrintWriter.println("Extraction finished at: " + LocalDateTime.now());
        statusPrintWriter.println("====================================================");
        statusPrintWriter.close();

    }

    public static boolean beforeGeometryEqualsAfterGeometry(OSMContribution contrib){
        try{
            Geometry before = contrib.getGeometryUnclippedBefore();
            Geometry after = contrib.getGeometryUnclippedAfter();

            OSMType thisType = contrib.getOSHEntity().getType();

            switch ( thisType ) {
                case RELATION:
                    return before.getCoordinates().equals( after.getCoordinates() );
                case NODE:
                case WAY:
                    return (before.equalsNorm(after));
            }
        }catch(Exception e){
            //We want to see this error.
            e.printStackTrace();
        }
        return false;
    }

    public static String creation(OSMContribution contrib){

        return "\"@e\":\"CRE\",";

    }

    public static String deletion(OSMContribution contrib){

        return  "\"@e\":\"DEL\"," +
                "\"@duid\":" + contrib.getEntityBefore().getUserId() + ",";

    }

    public static String minorVersion(OSMContribution contrib, int mV){

        MINOR_VERSION_CHANGE++;

        return "\"@e\":\"MIV\"," +
               "\"@mV\":"+ mV + "," + getSquaring(contrib);

    }

    public static String majorGeometry(OSMContribution contrib){

        MAJOR_GEOMETRY_CHANGE++;

        GeoJsonWriter writer = new GeoJsonWriter(18);
        writer.setEncodeCRS(false);

        return "\"@e\":\"MAG\"," + getSquaring(contrib);
    }

    public static String getGeometryString(OSMContribution contrib){
        GeoJsonWriter writer = new GeoJsonWriter(18);
        writer.setEncodeCRS(false);

        Geometry geom;

        try {
            if (contrib.getContributionTypes().contains(ContributionType.DELETION)) {
                geom = contrib.getGeometryUnclippedBefore();
            } else {
                geom = contrib.getGeometryUnclippedAfter();
            }

            if (geom.isValid() && !geom.isEmpty()) {
                return writer.write(geom);
            } else {
                return "null";
            }
        }catch(Exception e){
            System.err.println("Geometry Reconstruction Error: "+ contrib.getOSHEntity().getType() + " | " +contrib.getOSHEntity().getId());
            return "null";
        }
    }

    public static String getSquaring(OSMContribution contrib) {
        DecimalFormat numberFormat = new DecimalFormat("0.0000");

        String sq = "";
        if (contrib.getGeometryUnclippedAfter().getGeometryType().contains("Polygon")) {
            sq = "\"@sq\":" + numberFormat.format((avgSquareOffsetProjected(contrib.getGeometryUnclippedAfter()) -
                    avgSquareOffsetProjected(contrib.getGeometryUnclippedBefore()))) + ",";
        }
        return sq;
    }

    private static Coordinate projectToSphere(Coordinate coord){
        Deg2UTM coord2 = new Deg2UTM(coord.getY(), coord.getX());

        return new Coordinate(coord2.Easting,coord2.Northing,0.);
    }


    public static double avgSquareOffsetProjected(Geometry geom){

        Coordinate[] corners = geom.getCoordinates();

        if (corners.length > 2) {
            ArrayList<Double> cornerAngles = new ArrayList<>();
            for (int i = 2; i < corners.length; i++) {

                cornerAngles.add(toDegrees(angleBetween(projectToSphere( corners[i - 2] ), projectToSphere( corners[i - 1]) , projectToSphere( corners[i]))));
            }
            cornerAngles.add(toDegrees(angleBetween(projectToSphere( corners[corners.length - 2]), projectToSphere(corners[0]), projectToSphere( corners[1])))); //Remember, if it's closed -1 == 0;

            double avgError = 0;
            for (Double angle : cornerAngles) {
                //If the angle is closer to 180 than 90, then mod 90;
                if (angle > 135) {
                    angle = angle % 90;
                }
                avgError += Math.abs(angle - 90);
            }
            avgError /= cornerAngles.size();
//            GeoJsonWriter writer = new GeoJsonWriter(18);
//            writer.setEncodeCRS(false);
//            System.out.println(writer.write(geom) + "\nProj:" + avgError + " | " + cornerAngles);

            return avgError;

        }else{
            return 100;
        }
    }

    //https://stackoverflow.com/questions/176137/java-convert-lat-lon-to-utm
    private static class Deg2UTM
    {
        double Easting;
        double Northing;
        int Zone;
        char Letter;
        private  Deg2UTM(double Lat,double Lon)
        {
            Zone= (int) Math.floor(Lon/6+31);
            if (Lat<-72)
                Letter='C';
            else if (Lat<-64)
                Letter='D';
            else if (Lat<-56)
                Letter='E';
            else if (Lat<-48)
                Letter='F';
            else if (Lat<-40)
                Letter='G';
            else if (Lat<-32)
                Letter='H';
            else if (Lat<-24)
                Letter='J';
            else if (Lat<-16)
                Letter='K';
            else if (Lat<-8)
                Letter='L';
            else if (Lat<0)
                Letter='M';
            else if (Lat<8)
                Letter='N';
            else if (Lat<16)
                Letter='P';
            else if (Lat<24)
                Letter='Q';
            else if (Lat<32)
                Letter='R';
            else if (Lat<40)
                Letter='S';
            else if (Lat<48)
                Letter='T';
            else if (Lat<56)
                Letter='U';
            else if (Lat<64)
                Letter='V';
            else if (Lat<72)
                Letter='W';
            else
                Letter='X';
            Easting=0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180))/(1-Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180)))*0.9996*6399593.62/Math.pow((1+Math.pow(0.0820944379, 2)*Math.pow(Math.cos(Lat*Math.PI/180), 2)), 0.5)*(1+ Math.pow(0.0820944379,2)/2*Math.pow((0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180))/(1-Math.cos(Lat*Math.PI/180)*Math.sin(Lon*Math.PI/180-(6*Zone-183)*Math.PI/180)))),2)*Math.pow(Math.cos(Lat*Math.PI/180),2)/3)+500000;
            Easting=Math.round(Easting*100)*0.01;
            Northing = (Math.atan(Math.tan(Lat*Math.PI/180)/Math.cos((Lon*Math.PI/180-(6*Zone -183)*Math.PI/180)))-Lat*Math.PI/180)*0.9996*6399593.625/Math.sqrt(1+0.006739496742*Math.pow(Math.cos(Lat*Math.PI/180),2))*(1+0.006739496742/2*Math.pow(0.5*Math.log((1+Math.cos(Lat*Math.PI/180)*Math.sin((Lon*Math.PI/180-(6*Zone -183)*Math.PI/180)))/(1-Math.cos(Lat*Math.PI/180)*Math.sin((Lon*Math.PI/180-(6*Zone -183)*Math.PI/180)))),2)*Math.pow(Math.cos(Lat*Math.PI/180),2))+0.9996*6399593.625*(Lat*Math.PI/180-0.005054622556*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+4.258201531e-05*(3*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2))/4-1.674057895e-07*(5*(3*(Lat*Math.PI/180+Math.sin(2*Lat*Math.PI/180)/2)+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2))/4+Math.sin(2*Lat*Math.PI/180)*Math.pow(Math.cos(Lat*Math.PI/180),2)*Math.pow(Math.cos(Lat*Math.PI/180),2))/3);
            if (Letter<'M')
                Northing = Northing + 10000000;
            Northing=Math.round(Northing*100)*0.01;
        }
    }
}

