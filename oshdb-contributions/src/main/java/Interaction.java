import org.heigit.bigspatialdata.oshdb.InteractionsBuilder;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.util.celliterator.ContributionType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.text.DecimalFormat;
import java.util.ArrayList;

import static org.locationtech.jts.algorithm.Angle.angleBetween;
import static org.locationtech.jts.algorithm.Angle.toDegrees;

public class Interaction {

    DecimalFormat numberFormat = new DecimalFormat("0.0000");

    private OSMContribution thisContribution;
    private Geometry before;
    private Geometry after;

    public Interaction(OSMContribution contrib) {
        thisContribution = contrib;
        before = contrib.getGeometryUnclippedBefore();
        after  = contrib.getGeometryUnclippedAfter();
    }

    public boolean yieldsMinorVersion(OSMContribution contrib){
        if (! before.equals(after) ) {
            return true;
        }else {
            return false;
        }
    }

    public String getEditType(){
        if (thisContribution.getContributionTypes().contains(ContributionType.CREATION)) {
            return "\"@e\":\"CRE\"";
        } else if (thisContribution.getContributionTypes().contains(ContributionType.DELETION)) {
            return  "\"@e\":\"DEL\"," +
                    "\"@duid\":" + thisContribution.getEntityBefore().getUserId();
        } else if (thisContribution.getContributionTypes().contains(ContributionType.GEOMETRY_CHANGE)) {
            if (yieldsMinorVersion(thisContribution)) {
                return "\"@e\":\"MV\";
            }else{
                return "";//not a minor version
            }
        }
        return "";
    }

    public String getPrimaryProperties(){
        return  "\"@vS\":" + thisContribution.getTimestamp().getRawUnixTimestamp() + "," +
                "\"@uid\":" + thisContribution.getContributorUserId() + "," +
                "\"@id\":" + thisContribution.getOSHEntity().getId() + "," +
                "\"@c\":" + thisContribution.getChangesetId();
    }

    public String getSquareOffsetComparison(){
        return "\"@sq\":" +
                numberFormat.format( avgSquareOffsetProjected(thisContribution.getGeometryUnclippedAfter()) -
                                             avgSquareOffsetProjected(thisContribution.getGeometryUnclippedBefore()) );
    }



    public static String majorGeometry(OSMContribution contrib){

        Geometry before = contrib.getGeometryUnclippedBefore();
        Geometry after  = contrib.getGeometryUnclippedAfter();

        GeoJsonWriter writer = new GeoJsonWriter(18);
        writer.setEncodeCRS(false);

        DecimalFormat numberFormat = new DecimalFormat("0.0000");

        if (! before.equals(after) ) {



            String sq = "";
            if (contrib.getGeometryUnclippedAfter().getGeometryType().contains("Polygon") ){
                sq = "\"@sq\":"+ numberFormat.format( ( avgSquareOffsetProjected(after) - avgSquareOffsetProjected(before) ) )+",";
            }

            return "\"@e\":\"MG\"," + sq;
        }else{
            return "";
        }
    }





    private String getGeometryString(OSMContribution contrib){
        GeoJsonWriter writer = new GeoJsonWriter(18);
        writer.setEncodeCRS(false);

        //Put a safety lookup here to ensure it returns valid Geometries

        if ( contrib.getContributionTypes().contains(ContributionType.DELETION) ){
            return writer.write( contrib.getGeometryUnclippedBefore());
        }else{
            return writer.write( contrib.getGeometryUnclippedAfter());
        }
    }


    public static double avgSquareOffsetProjected(Geometry geom){

        Coordinate[] corners = geom.getCoordinates();

        if (corners.length > 2) {
            ArrayList<Double> cornerAngles = new ArrayList<Double>();
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


    private static Coordinate projectToSphere(Coordinate coord){
        Deg2UTM coord2 = new Deg2UTM(coord.getY(), coord.getX());

        return new Coordinate(coord2.Easting,coord2.Northing,0.);
    }
}
