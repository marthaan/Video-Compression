import java.io.File;

public class MyDecoder {
    File encoderFile;
    int n1;
    int n2;
    
    // constructor
    public MyDecoder(File encoderFile) {
        this.encoderFile = encoderFile;

        n1 = 0;
        n2 = 0; 
    }


    // ----- PART3: decompression -----

    // parse compressed input file --> get n1, n2, & coeffs. of each macroblock
    private void parseFile() {

    }

    // dequantize
    private void dequantize() {

    }


    // ----- PART4: decoding -----

    // IDCT
    private void idct() {

    }

    // display
    private void display() {

    }

    
    public static void main(String[] args) {
        File encoderFile = new File(args[0]);     // only input = MyEncoder output file 

        MyDecoder decoder = new MyDecoder(encoderFile);
    }
}
