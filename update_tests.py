import sys

file_path = "src/test/java/org/opensourcephysics/cabrillo/tracker/calibration/CalibrationTest.java"

with open(file_path, "r") as f:
    content = f.read()

content = content.replace("""    @Test
    public void testDegenerateScale() {
        assertThrows(IllegalArgumentException.class, () -> new Calibration(0.0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(Double.NaN, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(Double.POSITIVE_INFINITY, 0, 0, 0));
    }""", """    @Test
    public void testDegenerateScale() {
        assertThrows(IllegalArgumentException.class, () -> new Calibration(0.0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(Double.NaN, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(Double.POSITIVE_INFINITY, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(Double.NEGATIVE_INFINITY, 0, 0, 0));
    }""")

content = content.replace("""    @Test
    public void testDegenerateOrigin() {
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, Double.NaN, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, Double.POSITIVE_INFINITY, 0, 0));
    }""", """    @Test
    public void testDegenerateOrigin() {
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, Double.NaN, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, Double.POSITIVE_INFINITY, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, Double.NEGATIVE_INFINITY, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, Double.POSITIVE_INFINITY, 0));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, Double.NEGATIVE_INFINITY, 0));
    }""")

content = content.replace("""    @Test
    public void testDegenerateAngle() {
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, 0, Double.NaN));
    }""", """    @Test
    public void testDegenerateAngle() {
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, 0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, 0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Calibration(1.0, 0, 0, Double.NEGATIVE_INFINITY));
    }""")

with open(file_path, "w") as f:
    f.write(content)
