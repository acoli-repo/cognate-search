import java.io.*;
import java.util.*;

public class Split {
	public static void main(String[] argv) throws Exception {
		double train = 0.8;
		double dev  = 0.0;
		
		System.err.println("synopsis: Split PREFIX SUFFIX [TRAIN [DEV [TEST]]]\n"+
			"\tPREFIX, SUFFIX Split will create up to three output files:\n"+
			"\t               \"PREFIX\".train.\"SUFFIX\",\n"+
			"\t               \"PREFIX\".dev.\"SUFFIX\" (optional),\n"+
			"\t               \"PREFIX\".test.\"SUFFIX\"\n"+
			"\tTRAIN          relative portion of the training data, defaults to "+train+"\n"+
			"\tDEV            relative portion of the development data, defaults to "+dev+"\n"+
			"\tTEST           relative portion of the test data, defaults to max(0; 1.0-TRAIN-DEV)\n"+
			"\t               if these numbers don't add up to 1.0, they are normalized\n"+
			"Performs a reproducible random split of the input data.");

		String PREFIX = argv[0];
		String SUFFIX = argv[1];
		if(argv.length>2) train = Double.parseDouble(argv[2]);
		if(argv.length>3) dev = Double.parseDouble(argv[3]);
		double test = Math.max(0.0, 1.0-train-dev);
		if(argv.length>4) test = Double.parseDouble(argv[4]);
		if(train+dev+test!=1.0) {
			double sum = train+dev+test;
			train=train/sum;
			dev=dev/sum;
			test=test/sum;
		}

		System.err.println("using the following split: train "+train+", dev "+dev+", test "+test);
		Random rand = new Random(1000); // static initialization
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		Writer outTrain = new FileWriter(PREFIX+".train."+SUFFIX);
		Writer outDev = null;
		if(dev>0.0) outDev = new FileWriter(PREFIX+".dev."+SUFFIX);
		Writer outTest = null;
		if(test>0.0) outTest = new FileWriter(PREFIX+".test."+SUFFIX);
		for(String line=in.readLine(); line!=null; line=in.readLine()) {
			double r = rand.nextDouble();
			if(r<=train) {
				outTrain.write(line+"\n");
				outTrain.flush();
			} else if(r<=train+dev) {
				outDev.write(line+"\n");
				outDev.flush();
			} else {
				outTest.write(line+"\n");
				outTest.flush();
			}
		}
		
		outTrain.close();
		if(dev>0.0) outDev.close();
		if(test>0.0) outTest.close();
	}
}