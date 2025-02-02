package fishcute.celestial.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fishcute.celestial.sky.CelestialSky;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;

public class Util {

    static Random random = new Random();

    static DecimalFormat numberFormat = new DecimalFormat("#.00000000");
    public static double solveEquation(String str, Map<String, DynamicValue> toReplace) {
        if (toReplace.size() == 0 || str.equals(""))
            return 0;

        // Checks if the string is numeric
        try {
            return Double.parseDouble(str);
        }
        catch(NumberFormatException ignored) {}

        StringBuilder builder = new StringBuilder(str);
        for (String i : toReplace.keySet()) {
            while (builder.indexOf(i) != -1)
                builder.replace(builder.indexOf(i), builder.indexOf(i) + i.length(), numberFormat.format(Double.valueOf(toReplace.get(i).getValue())));
        }

        // Checks for #isUsing
        for (int index = builder.indexOf("#isUsing"); index >= 0; index = builder.indexOf("#isUsing", index + 1)) {
            result = parseStringVariable(builder.toString(), index, 8);
            if (result != null)
                builder.replace(index, index + result.length() + 10, numberFormat.format(isUsing(result) ? 1 : 0));
            else
                sendErrorInGame("Failed to parse #isUsing variable at index " + index, false);
        }

        // Checks for #isMiningWith
        for (int index = builder.indexOf("#isMiningWith"); index >= 0; index = builder.indexOf("#isMiningWith", index + 1)) {
            result = parseStringVariable(builder.toString(), index, 13);
            if (result != null)
                builder.replace(index, index + result.length() + 15, numberFormat.format(isMiningWith(result) ? 1 : 0));
            else
                sendErrorInGame("Failed to parse #isMiningWith variable at index " + index, false);
        }

        // Checks for #isHolding
        for (int index = builder.indexOf("#isHolding"); index >= 0; index = builder.indexOf("#isHolding", index + 1)) {
            result = parseStringVariable(builder.toString(), index, 10);
            if (result != null)
                builder.replace(index, index + result.length() + 12, numberFormat.format(isHolding(result) ? 1 : 0));
            else
                sendErrorInGame("Failed to parse #isHolding variable at index " + index, false);
        }

        // Checks for #distanceToArea
        for (int index = builder.indexOf("#distanceToArea"); index >= 0; index = builder.indexOf("#distanceToArea", index + 1)) {
            result = parseStringVariable(builder.toString(), index, 15);
            if (result != null)
                builder.replace(index, index + result.length() + 17, numberFormat.format(getDistanceToArea(result)));
            else
                sendErrorInGame("Failed to parse #isInArea variable at index " + index, false);
        }

        // Checks for #distanceTo
        for (int index = builder.indexOf("#distanceTo"); index >= 0; index = builder.indexOf("#distanceTo", index + 1)) {
            result = parseStringVariable(builder.toString(), index, 11);
            if (result != null)
                builder.replace(index, index + result.length() + 13, numberFormat.format(distanceTo(result)));
            else
                sendErrorInGame("Failed to parse #distanceTo variable at index " + index, false);
        }

        // Checks for #isInArea
        for (int index = builder.indexOf("#isInArea"); index >= 0; index = builder.indexOf("#isInArea", index + 1)) {
            result = parseStringVariable(builder.toString(), index, 9);
            if (result != null)
                builder.replace(index, index + result.length() + 11, numberFormat.format(isInArea(result) ? 1 : 0));
            else
                sendErrorInGame("Failed to parse #isInArea variable at index " + index, false);
        }

        // Checks again if the string is completely numeric
        try {
            return Double.parseDouble(str);
        }
        catch(NumberFormatException ignored) {}

        String finalStr = builder.toString();

        return new Equation(finalStr).parse();
    }

    static String result;
    static int foundIndex;

    static String parseStringVariable(String str, int index, int functionLength) {
        for (foundIndex = index; foundIndex < str.length() - 1; foundIndex++)
            if (str.charAt(foundIndex) == ')') {
                break;
            }
            else if (foundIndex == str.length() - 1) {
                foundIndex = -1;
                break;
            }

        if (foundIndex > 0 && ((index + functionLength + 1) < foundIndex)) {
            return str.substring(index + functionLength + 1, foundIndex);
        }
        return null;
    }

    /*
    Function below originally made by Boann on StackOverFlow, and slightly modified by me.
    */
    private static class Equation {
        public Equation(String finalStr) {
            this.finalStr = finalStr;
        }
        final String finalStr;
        boolean foundIssue = false;
        int pos = -1, ch;

        void nextChar() {
            ch = (++pos < finalStr.length()) ? finalStr.charAt(pos) : -1;
        }

        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        double parse() {
            nextChar();
            double x = parseExpression();
            if (pos < finalStr.length()) {
                if (!foundIssue) {
                    sendErrorInGame("Failed to perform math function \"" + finalStr + "\": Unexpected character '" + (char) ch + "'", false);
                    foundIssue = true;
                }
                return 0;
            }
            if (foundIssue)
                return 0;
            return x;
        }

        double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if      (eat('+')) x += parseTerm(); // addition
                else if (eat('-')) x -= parseTerm(); // subtraction
                else return x;
            }
        }

        double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if      (eat('*')) x *= parseFactor(); // multiplication
                else if (eat('/')) x /= parseFactor(); // division
                else return x;
            }
        }

        double parseFactor() {
            if (eat('+')) return +parseFactor(); // unary plus
            if (eat('-')) return -parseFactor(); // unary minus

            double x;
            int startPos = this.pos;
            if (eat('(')) { // parentheses
                x = parseExpression();
                if (!eat(')')) {
                    if (!foundIssue) {
                        sendErrorInGame("Failed to perform math function \"" + finalStr + "\": Missing closing parenthesis in function", false);
                        foundIssue = true;
                    }
                    return 0;
                }
            } else if ((ch >= '0' && ch <= '9') || ch == '.' || ch == ',') { // numbers
                while ((ch >= '0' && ch <= '9') || ch == '.' || ch == ',') nextChar();
                x = parseDouble(finalStr.substring(startPos, this.pos));
            } else if (ch >= 'a' && ch <= 'z') { // functions
                while (ch >= 'a' && ch <= 'z') nextChar();
                String func = finalStr.substring(startPos, this.pos);
                if (eat('(')) {
                    x = parseExpression();
                    if (!eat(')')) {
                        if (!foundIssue) {
                            sendErrorInGame("Failed to perform math function \"" + finalStr + "\": Missing closing parenthesis in function after argument to \"" + func + "\"", false);
                            foundIssue = true;
                        }
                        return 0;
                    }
                } else {
                    x = parseFactor();
                }

                switch (func) {
                    case "sqrt":
                        x = Math.sqrt(x);
                        break;
                    case "sin":
                        x = Math.sin(Math.toRadians(x));
                        break;
                    case "cos":
                        x = Math.cos(Math.toRadians(x));
                        break;
                    case "tan":
                        x = Math.tan(Math.toRadians(x));
                        break;
                    case "floor":
                        x = Math.floor(x);
                        break;
                    case "ceil":
                        x = Math.ceil(x);
                        break;
                    case "round":
                        x = Math.round(x);
                        break;
                    case "print":
                        print(x);
                        break;
                    case "printnv":
                        print(x);
                        x = 0;
                        break;
                    default: {
                        if (!foundIssue) {
                            sendErrorInGame("Failed to perform math function \"" + finalStr + "\": Unknown math function \"" + func + "\"", false);
                            foundIssue = true;
                        }
                        return 0;
                    }
                }
            } else {
                if (!foundIssue) {
                    sendErrorInGame("Failed to perform math function \"" + finalStr + "\": Unexpected character '" + (char) ch + "'", false);
                    foundIssue = true;
                }
                return 0;
            }

            if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation
            else if (eat('m')) x = Math.min(x, parseFactor()); // min
            else if (eat('M')) x = Math.max(x, parseFactor()); // max
            else if (eat('&')) x = (x == 1 && parseFactor() == 1) ? 1 : 0; // and
            else if (eat('|')) x = (or(x, parseFactor())) ? 1 : 0; // or
            else if (eat('=')) x = (x == parseFactor()) ? 1 : 0; // and
            else if (eat('>')) x = (x > parseFactor()) ? 1 : 0;
            else if (eat('<')) x = (x < parseFactor()) ? 1 : 0;

            return x;
        }
    }

    // For some reason things won't work if I don't do this
    static boolean or(double a, double b) {
        return a == 1 || b == 1;
    }

    static double parseDouble(String i) {
        return Double.parseDouble(i.contains(",") ? i.replaceAll(",", ".") : i);
    }

    static void print(double i) {
        Minecraft.getInstance().player.displayClientMessage(Component.literal("Value: " + i), true);
    }
    public static void log(Object i) {
        if (!Minecraft.getInstance().isPaused())
            System.out.println("[Celestial] " + i.toString());
    }

    public static void warn(Object i) {
        CelestialSky.warnings++;
        if (!Minecraft.getInstance().isPaused()) {
            log("[Warn] " + i.toString());
            sendWarnInGame(i.toString());
        }
    }

    public static ArrayList<String> errorList = new ArrayList<>();

    public static void sendErrorInGame(String i, boolean unloadResources) {
        CelestialSky.errors++;
        if (Minecraft.getInstance().player == null)
            return;
        if (errorList.contains(i) || errorList.size() > 25)
            return;
        errorList.add(i);
        Minecraft.getInstance().player.displayClientMessage(Component.literal(ChatFormatting.RED +
                "[Celestial] " + i
        ), false);

        if (errorList.size() >= 25)
            Minecraft.getInstance().player.displayClientMessage(Component.literal(ChatFormatting.RED +
                    "[Celestial] Passing 25 error messages. Muting error messages."
            ), false);

        if (unloadResources) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal(ChatFormatting.RED +
                    "[Celestial] Unloading Celestial resources."
            ), false);
        }
    }

    public static void sendWarnInGame(String i) {
        if (Minecraft.getInstance().player == null)
            return;
        if (errorList.contains(i))
            return;
        errorList.add(i);
        Minecraft.getInstance().player.displayClientMessage(Component.literal(ChatFormatting.YELLOW +
                "[Celestial] " + i
        ), false);
    }

    public static boolean getOptionalBoolean(JsonObject o, String toGet, boolean ifNull) {
        return o != null && o.has(toGet) ? o.get(toGet).getAsBoolean() : ifNull;
    }

    public static String getOptionalString(JsonObject o, String toGet, String ifNull) {
        return o != null && o.has(toGet) ? o.get(toGet).getAsString() : ifNull;
    }

    public static double getOptionalDouble(JsonObject o, String toGet, double ifNull) {
        return o != null && o.has(toGet) ? o.get(toGet).getAsDouble() : ifNull;
    }

    public static int getOptionalInteger(JsonObject o, String toGet, int ifNull) {
        return o != null && o.has(toGet) ? o.get(toGet).getAsInt() : ifNull;
    }

    public static ArrayList<String> getOptionalStringArray(JsonObject o, String toGet, ArrayList<String> ifNull) {
        return o != null && o.has(toGet) ? convertToStringArrayList(o.get(toGet).getAsJsonArray()) : ifNull;
    }

    public static ArrayList<String> convertToStringArrayList(JsonArray array) {
        ArrayList<String> toReturn = new ArrayList<>();
        for (JsonElement o : array) {
            toReturn.add(o.getAsString());
        }
        return toReturn;
    }

    public static int getDecimal(String hex){
        String digits = "0123456789ABCDEF";
        hex = hex.toUpperCase();
        int val = 0;
        for (int i = 0; i < hex.length(); i++)
        {
            char c = hex.charAt(i);
            int d = digits.indexOf(c);
            val = 16*val + d;
        }
        return val;
    }

    public static int getDecimal(Color color) {
        return getDecimal(String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
    }

    public static double generateRandomDouble(double min, double max) {
        return min + ((max - min) * random.nextDouble());
    }

    public static Color decodeColor(String hex) {
        try {
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        }
        catch (Exception ignored) {
            sendErrorInGame("Failed to parse HEX color \"" + hex + "\"", false);
            return new Color(0, 0, 0);
        }
    }

    static final HashMap<String, DynamicValue> toReplaceMap = new HashMap<>();
    public static void initalizeToReplaceMap(HashMap<String, DynamicValue> extraValues) {
        //new DynamicValue() {@Override double getValue() {return ;}};

        toReplaceMap.clear();

        toReplaceMap.put("#xPos", new DynamicValue() {@Override public double getValue() { return
                Minecraft.getInstance().player.getX();
        }});
        toReplaceMap.put("#yPos", new DynamicValue() {@Override public double getValue() { return
                Minecraft.getInstance().player.getY();
        }});
        toReplaceMap.put("#zPos", new DynamicValue() {@Override public double getValue() { return
                Minecraft.getInstance().player.getZ();
        }});
        toReplaceMap.put("#tickDelta", new DynamicValue() {@Override public double getValue() { return
                Minecraft.getInstance().getFrameTime();
        }});
        toReplaceMap.put("#dayLight", new DynamicValue() {@Override public double getValue() { return
                1.0F - Minecraft.getInstance().level.getStarBrightness(Minecraft.getInstance().getFrameTime());
        }});
        toReplaceMap.put("#rainGradient", new DynamicValue() {@Override public double getValue() { return
                1.0F - Minecraft.getInstance().level.getRainLevel(Minecraft.getInstance().getFrameTime());
        }});
        toReplaceMap.put("#isSubmerged", new DynamicValue() {@Override public double getValue() { return
                Minecraft.getInstance().player.isInWater() ? 1 : 0;
        }});
        toReplaceMap.put("#getTotalTime", new DynamicValue() {@Override public double getValue() { return
                Minecraft.getInstance().level.getGameTime();
        }});
        toReplaceMap.put("#getDayTime", new DynamicValue() {@Override public double getValue() { return
                Minecraft.getInstance().level.dayTime() - (Math.floor(Minecraft.getInstance().level.dayTime() / 24000f) * 24000);
        }});
        toReplaceMap.put("#starAlpha", new DynamicValue() {@Override public double getValue() { return
                Minecraft.getInstance().level.getStarBrightness(Minecraft.getInstance().getFrameTime());
        }});
        toReplaceMap.put("#random", new DynamicValue() {@Override public double getValue() { return
                Math.random();
        }});
        toReplaceMap.put("#skyAngle", new DynamicValue() {@Override public double getValue() { return
                Minecraft.getInstance().level.getTimeOfDay(Minecraft.getInstance().getFrameTime()) * 360.0F;
        }});
        toReplaceMap.put("#maxInteger", new DynamicValue() {@Override public double getValue() { return
                Integer.MAX_VALUE;
        }});
        toReplaceMap.put("#yaw", new DynamicValue() {@Override public double getValue() { return
                Minecraft.getInstance().player.getViewXRot(Minecraft.getInstance().getFrameTime());
        }});
        toReplaceMap.put("#pitch", new DynamicValue() {@Override public double getValue() { return
                Minecraft.getInstance().player.getViewYRot(Minecraft.getInstance().getFrameTime());
        }});
        toReplaceMap.put("#isLeftClicking", new DynamicValue() {@Override public double getValue() { return
                isLeftClicking() ? 1 : 0;
        }});
        toReplaceMap.put("#isRightClicking", new DynamicValue() {@Override public double getValue() { return
                isRightClicking() ? 1 : 0;
        }});

        toReplaceMap.putAll(extraValues);
    }
    public static HashMap<String, DynamicValue> getReplaceMapNormal() {
        return toReplaceMap;
    }

    public static HashMap<String, DynamicValue> getReplaceMapAdd(Map<String, Double> extraEntries) {
        HashMap<String, DynamicValue> toReturn = (HashMap<String, DynamicValue>) getReplaceMapNormal().clone();
        for (String i : extraEntries.keySet()) {
            toReturn.put(i, new DynamicValue() {@Override public double getValue() { return
                    extraEntries.get(i);
            }});
        }
        return toReturn;
    }

    public static abstract class DynamicValue {
        public abstract double getValue();
    }

    //I have nightmares about ArrayList<MutablePair<MutableTriple<String, String, String>, MutablePair<String, String>>>
    //This was a mistake
    public static ArrayList<MutablePair<MutableTriple<String, String, String>, MutablePair<String, String>>> convertToPointUvList(ArrayList<String> array) {
        //There's always a better way to do things, and here, I really don't care.

        ArrayList<MutablePair<MutableTriple<String, String, String>, MutablePair<String, String>>> returnArray = new ArrayList<>();
        String[] splitString1;
        String[] splitString2;
        try {
            if (array == null)
                return new ArrayList<>();
            for (String i : array) {
                //1, 2, 3 : 1, 2

                // If there is UV stuff
                if (i.contains(":")) {

                    //vertex points
                    String a = i.split(":")[0];

                    //uv
                    String b = i.split(":")[1];

                    splitString1 = a.split(",");
                    splitString2 = b.split(",");
                    returnArray.add(new MutablePair<>(new MutableTriple<>(splitString1[0], splitString1[1], splitString1[2]), new MutablePair<>(splitString2[0], splitString2[1])));
                }
                // If there is no UV stuff
                else {
                    splitString1 = i.split(",");
                    returnArray.add(new MutablePair<>(new MutableTriple<>(splitString1[0], splitString1[1], splitString1[2]), new MutablePair<>("0", "0")));
                }
            }
            return returnArray;
        }
        catch (Exception e) {
            warn("Failed to parse vertex point array \"" + array.toString() + "\"");
            return new ArrayList<>();
        }
    }

    static String[] a;

    public static boolean isUsing(String item) {
        return Minecraft.getInstance().mouseHandler.isRightPressed() && isHolding(item);
    }

    public static boolean isMiningWith(String item) {
        return Minecraft.getInstance().mouseHandler.isLeftPressed() && isHolding(item);
    }

    public static boolean isRightClicking() {
        return Minecraft.getInstance().mouseHandler.isRightPressed();
    }

    public static boolean isLeftClicking() {
        return Minecraft.getInstance().mouseHandler.isLeftPressed();
    }

    public static boolean isHolding(String item) {
        if (item.contains(":")) {
            a = item.split(":");
            return (Registry.ITEM.getKey(Minecraft.getInstance().player.getMainHandItem().getItem()).getNamespace().equals(a[0])) &&
                    (Registry.ITEM.getKey(Minecraft.getInstance().player.getMainHandItem().getItem()).getPath().equals(a[1]));
        }
        else {
            return (Registry.ITEM.getKey(Minecraft.getInstance().player.getMainHandItem().getItem()).getPath().equals(item));
        }
    }

    public static boolean isInArea(String arguments) {
        try {
            String[] str = arguments.split(",");

            return (Double.parseDouble(str[0]) <= Minecraft.getInstance().player.getX() && Minecraft.getInstance().player.getX() <= Double.parseDouble(str[3]) + 1) &&
                    (Double.parseDouble(str[1]) <= Minecraft.getInstance().player.getY() && Minecraft.getInstance().player.getY() <= Double.parseDouble(str[4]) + 1) &&
                    (Double.parseDouble(str[2]) <= Minecraft.getInstance().player.getZ() && Minecraft.getInstance().player.getZ() <= Double.parseDouble(str[5]) + 1);
        }
        catch (Exception e) {
            sendErrorInGame("Failed to parse #isInArea variable with arguments \"" + arguments + "\".", false);
            return false;
        }
    }

    public static double distanceTo(String arguments) {
        try {
            String[] str = arguments.split(",");

            return Math.sqrt((Minecraft.getInstance().player.getX() - Double.parseDouble(str[0])) *
                        (Minecraft.getInstance().player.getX() - Double.parseDouble(str[0])) +
                        (Minecraft.getInstance().player.getY() - Double.parseDouble(str[1])) *
                        (Minecraft.getInstance().player.getY() - Double.parseDouble(str[1])) +
                        (Minecraft.getInstance().player.getZ() - Double.parseDouble(str[2])) *
                        (Minecraft.getInstance().player.getZ() - Double.parseDouble(str[2])));
            }
        catch (Exception e) {
            sendErrorInGame("Failed to parse #distanceTo variable with arguments \"" + arguments + "\".", false);
            return 0;
        }
    }

    static float getDistance(float a, float b){
        if (a <= 0) {
            if (b <= 0) return 0F;
            else return b;
        } else if (b <= 0)
            return a;
        return ((float) Math.sqrt(((double) a * a + b * b)));
    }

    public static double getDistanceToArea(String arguments) {
        try {
            String[] str = arguments.split(",");

            return getDistanceToArea(
                    Double.parseDouble(str[0]),
                    Double.parseDouble(str[1]),
                    Double.parseDouble(str[2]),
                    Double.parseDouble(str[3]),
                    Double.parseDouble(str[4]),
                    Double.parseDouble(str[5]));
        }
        catch (Exception e) {
            sendErrorInGame("Failed to parse #distanceToArea variable with arguments \"" + arguments + "\".", false);
            return 0;
        }
    }

    public static double getDistanceToArea(double x1, double y1, double z1, double x2, double y2, double z2) {
        double minX = Float.min((float) x1, (float) x2);
        double maxX = Float.max((float) x1, (float) x2);
        double minY = Float.min((float) y1, (float) y2);
        double maxY = Float.max((float) y1, (float) y2);
        double minZ = Float.min((float) z1, (float) z2);
        double maxZ = Float.max((float) z1, (float) z2);

        float[] axisDistances = new float[3];

        {
            double min = minX - Minecraft.getInstance().player.getX();
            double max = Minecraft.getInstance().player.getX() - maxX;
            axisDistances[0] = Float.max((float) min, (float) max);
        }
        {
            double min = minY - Minecraft.getInstance().player.getY();
            double max = Minecraft.getInstance().player.getY() - maxY;
            axisDistances[1] = Float.max((float) min, (float) max);
        }
        {
            double min = minZ - Minecraft.getInstance().player.getZ();
            double max = Minecraft.getInstance().player.getZ() - maxZ;
            axisDistances[2] = Float.max((float) min, (float) max);
        }

        return getDistance(
                getDistance(
                        axisDistances[0],
                        axisDistances[1]
                ),
                axisDistances[2]
        );
    }
}
