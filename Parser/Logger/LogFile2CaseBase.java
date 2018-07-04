 package CasebaseCreation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.*;

import org.jLOAF.action.AtomicAction;
import org.jLOAF.casebase.Case;
import org.jLOAF.casebase.CaseBase;
import org.jLOAF.inputs.AtomicInput;
import org.jLOAF.inputs.ComplexInput;
import org.jLOAF.inputs.Feature;
import org.jLOAF.sim.AtomicSimilarityMetricStrategy;
import org.jLOAF.sim.ComplexSimilarityMetricStrategy;
import org.jLOAF.sim.SimilarityMetricStrategy;
import org.jLOAF.sim.StateBasedSimilarity;
import org.jLOAF.sim.StateBased.KOrderedSimilarity;
import org.jLOAF.sim.StateBased.OrderedSimilarity;
import org.jLOAF.sim.atomic.EuclideanDistance;
import org.jLOAF.sim.complex.GreedyMunkrezMatching;
import org.jLOAF.sim.complex.Mean;
import org.jLOAF.sim.complex.WeightedMean;
import org.jLOAF.weights.SimilarityWeights;
import org.jLOAF.weights.Weights;

import AgentModules.RoboCupAction;
import AgentModules.RoboCupInput;
/**
 * @author sacha gunaratne 
 * @since 2017 may
 * 
 * @Constructor: None
 * @Methods:
 * logParser: converts a logfile into a casebase and writes the casebase to a file
 * **/
public class LogFile2CaseBase {
	
	/**
	 * Converts a logfile into a CaseBase and writes it to a file.
	 * 
	 * This method uses Regex to identify important pieces of the logfile such as ball, goal, and flags and extract their direction and angle in relation to the player
	 * These are placed into a input structure as the following. 
	 * 					  StateBasedInput
	 * 						    |
	 * ComplexInput 		 RobocupInput
	 * 						/	  |	      \
	 * ComplexInput		Goal	 Flags      Ball
	 * 					/ \       /  \        / \
	 * AtomicInput     Dir Dist Dir Dist  Dir Dist
	 *					|   |    |   |     |   |
	 * Feature         .f  .f    .f  .f    .f  .f
	 * 
	 * Each input has a specific SimilarityMetric. 
	 * RoboCupInput has a weightedMean SimilarityMetric where the weights are initialized as default(w==0).
	 * They have been manually updated on line 303-307. They can be manually solved for using any WeightSelectiuon Algorithm. 
	 * 
	 * The input is created and if there is an action that exists on the next line, it is read as well.
	 * 
	 * The actions follow a similar structure:
	 * 	ComplexAction			RoboCupAction
	 * 								|
	 * 	AtomicAction 			turnAngle
	 *  							|
	 *  Feature					   .f
	 * 
	 * The complexAction RoboCupAction can contain multiple AtomicActions, depending on the situation. Kick has two AtomicActions - dir and power
	 * 
	 * When both an action and input have been created, they will be added to the CaseBase.
	 * 
	 * Once, the logfile has been comepltely read through, the caseBase is written to file. 
	 * 
	 * @param logfile Logfile containing trace information
	 * @param outfile the output path of the CaseBase
	 * 
	 *  @return nothing
	 *  
	 *  @author sachagunaratne
	 *  @since May 2017
	 * 
	 * ***/
	public void logParser(String logfile, String outfile) throws IOException{
		
		String [] flagPatterns = new String[45];
		String [] flagPattern_Names = {"fcb", "flb","frb", "fct","flt", "frt", "fc","fplt", "fplc", "fplb", "fprt", "fprc", "fprb","ftl50","ftr50","fbl50","fbr50","ftl40","ftr40","fbl40","fbr40","ftl30","ftr30","fbl30","fbr30","ftl20","ftr20","fbl20","fbr20","ftl10","ftr10","fbl10","fbr10","frt30","frb30","flt30","flb30","frt20","frb20","flt20","flb20","frt10","frb10","flt10","flb10"};
		
		//patterns
		String visualPattern = "\\(see (?<time>[\\d]{0,4}) .*\\)";
		String goalPattern = "\\(\\(g (?<goalSide>[r,l]\\)) (?<goalDistance>[\\d,\\.]+) (?<goalAngle>[\\-,\\d]+)";
		String ballPattern = "\\(\\(b\\) (?<ballDistance>[\\d,\\.]+) (?<ballAngle>[\\-,\\d]+)";
		String ballPattern2 = "\\(\\(B\\) (?<ballDistance2>[\\d,\\.]+) (?<ballAngle2>[\\-,\\d]+)";
		String actionPattern = "^\\(\\b(?<action>kick|turn|dash)\\b";
		String turnPattern = "\\(turn (?<turnAngle>[\\-,\\d,\\.]+)";
		String DashPattern = "\\(dash (?<DashPower>[\\-,\\d,\\.]+)";
		String KickPattern = "\\(kick (?<KickPower>[\\-,\\d,\\.]+) (?<KickAngle>[\\-,\\d,\\.]+)";
		//flags
		flagPatterns[0] ="\\(\\(f c b\\) (?<fcbDistance>[\\d,\\.]+) (?<fcbAngle>[\\-,\\d]+)";
		flagPatterns[1] ="\\(\\(f l b\\) (?<flbDistance>[\\d,\\.]+) (?<flbAngle>[\\-,\\d]+)";
		flagPatterns[2] ="\\(\\(f r b\\) (?<frbDistance>[\\d,\\.]+) (?<frbAngle>[\\-,\\d]+)";
		flagPatterns[3] ="\\(\\(f c t\\) (?<fctDistance>[\\d,\\.]+) (?<fctAngle>[\\-,\\d]+)";
		flagPatterns[4] ="\\(\\(f l t\\) (?<fltDistance>[\\d,\\.]+) (?<fltAngle>[\\-,\\d]+)";
		flagPatterns[5] ="\\(\\(f r t\\) (?<frtDistance>[\\d,\\.]+) (?<frtAngle>[\\-,\\d]+)";
		flagPatterns[6] ="\\(\\(f c\\) (?<fcDistance>[\\d,\\.]+) (?<fcAngle>[\\-,\\d]+)";
		flagPatterns[7] ="\\(\\(f p l t\\) (?<fpltDistance>[\\d,\\.]+) (?<fpltAngle>[\\-,\\d]+)";
		flagPatterns[8] ="\\(\\(f p l c\\) (?<fplcDistance>[\\d,\\.]+) (?<fplcAngle>[\\-,\\d]+)";
		flagPatterns[9] ="\\(\\(f p l b\\) (?<fplbDistance>[\\d,\\.]+) (?<fplbAngle>[\\-,\\d]+)";
		flagPatterns[10] ="\\(\\(f p r t\\) (?<fprtDistance>[\\d,\\.]+) (?<fprtAngle>[\\-,\\d]+)";
		flagPatterns[11] ="\\(\\(f p r c\\) (?<fprcDistance>[\\d,\\.]+) (?<fprcAngle>[\\-,\\d]+)";
		flagPatterns[12] ="\\(\\(f p r b\\) (?<fprbDistance>[\\d,\\.]+) (?<fprbAngle>[\\-,\\d]+)";
		flagPatterns[13] ="\\(\\(f t l 50\\) (?<ftl50Distance>[\\d,\\.]+) (?<ftl50Angle>[\\-,\\d]+)";
		flagPatterns[14] ="\\(\\(f t r 50\\) (?<ftr50Distance>[\\d,\\.]+) (?<ftr50Angle>[\\-,\\d]+)";
		flagPatterns[15] ="\\(\\(f b l 50\\) (?<fbl50Distance>[\\d,\\.]+) (?<fbl50Angle>[\\-,\\d]+)";
		flagPatterns[16] ="\\(\\(f b r 50\\) (?<fbr50Distance>[\\d,\\.]+) (?<fbr50Angle>[\\-,\\d]+)";
		flagPatterns[17] ="\\(\\(f t l 40\\) (?<ftl40Distance>[\\d,\\.]+) (?<ftl40Angle>[\\-,\\d]+)";
		flagPatterns[18] ="\\(\\(f t r 40\\) (?<ftr40Distance>[\\d,\\.]+) (?<ftr40Angle>[\\-,\\d]+)";
		flagPatterns[19] ="\\(\\(f b l 40\\) (?<fbl40Distance>[\\d,\\.]+) (?<fbl40Angle>[\\-,\\d]+)";
		flagPatterns[20] ="\\(\\(f b r 40\\) (?<fbr40Distance>[\\d,\\.]+) (?<fbr40Angle>[\\-,\\d]+)";
		flagPatterns[21] ="\\(\\(f t l 30\\) (?<ftl30Distance>[\\d,\\.]+) (?<ftl30Angle>[\\-,\\d]+)";
		flagPatterns[22] ="\\(\\(f t r 30\\) (?<ftr30Distance>[\\d,\\.]+) (?<ftr30Angle>[\\-,\\d]+)";
		flagPatterns[23] ="\\(\\(f b l 30\\) (?<fbl30Distance>[\\d,\\.]+) (?<fbl30Angle>[\\-,\\d]+)";
		flagPatterns[24] ="\\(\\(f b r 30\\) (?<fbr30Distance>[\\d,\\.]+) (?<fbr30Angle>[\\-,\\d]+)";
		flagPatterns[25] ="\\(\\(f t l 20\\) (?<ftl20Distance>[\\d,\\.]+) (?<ftl20Angle>[\\-,\\d]+)";
		flagPatterns[26] ="\\(\\(f t r 20\\) (?<ftr20Distance>[\\d,\\.]+) (?<ftr20Angle>[\\-,\\d]+)";
		flagPatterns[27] ="\\(\\(f b l 20\\) (?<fbl20Distance>[\\d,\\.]+) (?<fbl20Angle>[\\-,\\d]+)";
		flagPatterns[28] ="\\(\\(f b r 20\\) (?<fbr20Distance>[\\d,\\.]+) (?<fbr20Angle>[\\-,\\d]+)";
		flagPatterns[29] ="\\(\\(f t l 10\\) (?<ftl10Distance>[\\d,\\.]+) (?<ftl10Angle>[\\-,\\d]+)";
		flagPatterns[30] ="\\(\\(f t r 10\\) (?<ftr10Distance>[\\d,\\.]+) (?<ftr10Angle>[\\-,\\d]+)";
		flagPatterns[31] ="\\(\\(f b l 10\\) (?<fbl10Distance>[\\d,\\.]+) (?<fbl10Angle>[\\-,\\d]+)";
		flagPatterns[32] ="\\(\\(f b r 10\\) (?<fbr10Distance>[\\d,\\.]+) (?<fbr10Angle>[\\-,\\d]+)";
		flagPatterns[33] ="\\(\\(f r t 30\\) (?<frt30Distance>[\\d,\\.]+) (?<frt30Angle>[\\-,\\d]+)";
		flagPatterns[34] ="\\(\\(f r b 30\\) (?<frb30Distance>[\\d,\\.]+) (?<frb30Angle>[\\-,\\d]+)";
		flagPatterns[35] ="\\(\\(f l t 30\\) (?<flt30Distance>[\\d,\\.]+) (?<flt30bAngle>[\\-,\\d]+)";
		flagPatterns[36] ="\\(\\(f l b 30\\) (?<flb30Distance>[\\d,\\.]+) (?<flb30Angle>[\\-,\\d]+)";
		flagPatterns[37] ="\\(\\(f r t 20\\) (?<frt20Distance>[\\d,\\.]+) (?<frt20Angle>[\\-,\\d]+)";
		flagPatterns[38] ="\\(\\(f r b 20\\) (?<frb20Distance>[\\d,\\.]+) (?<frb20Angle>[\\-,\\d]+)";
		flagPatterns[39] ="\\(\\(f l t 20\\) (?<flt20Distance>[\\d,\\.]+) (?<flt20Angle>[\\-,\\d]+)";
		flagPatterns[40] ="\\(\\(f l b 20\\) (?<flb20Distance>[\\d,\\.]+) (?<flb20Angle>[\\-,\\d]+)";
		flagPatterns[41] ="\\(\\(f r t 10\\) (?<frt10Distance>[\\d,\\.]+) (?<frt10Angle>[\\-,\\d]+)";
		flagPatterns[42] ="\\(\\(f r b 10\\) (?<frb10Distance>[\\d,\\.]+) (?<frb10Angle>[\\-,\\d]+)";
		flagPatterns[43] ="\\(\\(f l t 10\\) (?<flt10Distance>[\\d,\\.]+) (?<flt10Angle>[\\-,\\d]+)";
		flagPatterns[44] ="\\(\\(f l b 10\\) (?<flb10Distance>[\\d,\\.]+) (?<flb10Angle>[\\-,\\d]+)";
		
		//pattern objects
		Pattern vp = Pattern.compile(visualPattern);
		Pattern gp = Pattern.compile(goalPattern);
		Pattern bp = Pattern.compile(ballPattern);
		Pattern bp2 = Pattern.compile(ballPattern2);
		Pattern ap = Pattern.compile(actionPattern);
		Pattern tp = Pattern.compile(turnPattern);
		Pattern dp = Pattern.compile(DashPattern);
		Pattern kp = Pattern.compile(KickPattern);
		
		//flag patterns
		Pattern flag;
		
		//matcher
		Matcher m;
		
		//inputs
		RoboCupAction action = null;
		ComplexInput ginput;
		ComplexInput binput;
		ComplexInput flaginput;
		ComplexInput flags;
		
		
		RoboCupInput input = null;
		
		//booleans
		boolean hasAction = false;
		boolean hasInput = false;
		boolean want_flags = true;
		//casebase
		CaseBase cb = new CaseBase();
		
		//similarityMetrics
		//atomic
		AtomicSimilarityMetricStrategy Atomic_strat = new EuclideanDistance();
		//complex
		ComplexSimilarityMetricStrategy ballGoal_strat = new Mean();
		ComplexSimilarityMetricStrategy flag_strat = new GreedyMunkrezMatching();
		//reactive
		StateBasedSimilarity stateBasedSim = new KOrderedSimilarity(1);
		
		//weights
		SimilarityWeights sim_weights = new SimilarityWeights(); 
		
		ComplexSimilarityMetricStrategy RoboCup_strat = new WeightedMean(sim_weights);
	
		try {
			BufferedReader br = new BufferedReader(new FileReader(logfile),'r');
			String Line;
			System.out.println("Creating casebase...");
			while ((Line = br.readLine()) != null){
				//check action
				m = ap.matcher(Line);
				if(m.find()){
					//System.out.println(m.group(1));
					action = new RoboCupAction(m.group(1));
					hasAction = true;
					
					if(m.group(1).equals("turn")){
						//check turnAngle
						m = tp.matcher(Line);
						if(m.find()){
							//System.out.println(m.group(1));
							AtomicAction turnAngle = new AtomicAction("turnAngle");
							turnAngle.setFeature(new Feature(Double.parseDouble(m.group(1))));
							action.add(turnAngle);
						}	
					}else if (m.group(1).equals("dash")){
						//check dashPower
						m = dp.matcher(Line);
						if(m.find()){
							//System.out.println(m.group(1));
							AtomicAction dashPower =new AtomicAction("dashPower");
							dashPower.setFeature(new Feature(Double.parseDouble(m.group(1))));
							action.add(dashPower);
						}
					}else if(m.group(1).equals("kick")){
						//check kickPower and kickAngle
						m = kp.matcher(Line);
						if(m.find()){
							//System.out.println(m.group(1));
							//System.out.println(m.group(2));
							AtomicAction kickPower = new AtomicAction("kickPower");
							kickPower.setFeature(new Feature(Double.parseDouble(m.group(1))));
							AtomicAction kickAngle = new AtomicAction("kickAngle"); 
							kickAngle.setFeature(new Feature(Double.parseDouble(m.group(2))));
							action.add(kickPower);
							action.add(kickAngle);
						}
					}
				}
				//check perception
				m = vp.matcher(Line);
				if(m.find()){
					input = new RoboCupInput("SenseEnvironment", RoboCup_strat);
					hasInput = true;
					//check goalDistance and goalAngle
					m = gp.matcher(Line);
					if(m.find()){
						ginput = new ComplexInput("goal "+m.group(1).replace(")", ""), ballGoal_strat);
						//System.out.println(m.group(1).replace(')', ' '));
						//System.out.println(m.group(2));
						//System.out.println(m.group(3));
						Feature goalDist = new Feature(Double.parseDouble(m.group(2))); 
						Feature goalAngle = new Feature(Double.parseDouble(m.group(3)));
						ginput.add(new AtomicInput("goal_dist", goalDist, Atomic_strat));
						ginput.add(new AtomicInput("goal_dir", goalAngle, Atomic_strat));
						
						//add to input
						input.add(ginput);					
					}
					//check BallDistance and ballAngle
					m = bp.matcher(Line);
					if(m.find()){
						binput = new ComplexInput("ball",ballGoal_strat);
						//System.out.println(m.group(1));
						//System.out.println(m.group(2));
						Feature ballDist = new Feature(Double.parseDouble(m.group(1))); 
						Feature ballAngle = new Feature(Double.parseDouble(m.group(2)));
						binput.add(new AtomicInput("ball_dist", ballDist, Atomic_strat));
						binput.add(new AtomicInput("ball_dir", ballAngle, Atomic_strat));
						
						//add to input
						input.add(binput);	
					}
					
					//check BallDistance and ballAngle
					m = bp2.matcher(Line);
					if(m.find()){
						binput = new ComplexInput("ball",ballGoal_strat);
						//System.out.println(m.group(1));
						//System.out.println(m.group(2));
						Feature ballDist = new Feature(Double.parseDouble(m.group(1))); 
						Feature ballAngle = new Feature(Double.parseDouble(m.group(2)));
						binput.add(new AtomicInput("ball_dist", ballDist, Atomic_strat));
						binput.add(new AtomicInput("ball_dir", ballAngle, Atomic_strat));
						
						//add to input
						input.add(binput);	
					}
					
					if(want_flags){
						flags = new ComplexInput("flags",flag_strat);
						for(int i =0;i<flagPatterns.length;i++){
							flag = Pattern.compile(flagPatterns[i]);
							m = flag.matcher(Line);
							if(m.find()){
								flaginput = new ComplexInput(flagPattern_Names[i], ballGoal_strat);
								
								Feature Dist = new Feature(Double.parseDouble(m.group(1))); 
								Feature Angle = new Feature(Double.parseDouble(m.group(2)));
								flaginput.add(new AtomicInput(flagPattern_Names[i]+"_dist", Dist,Atomic_strat));
								flaginput.add(new AtomicInput(flagPattern_Names[i]+"_dir", Angle,Atomic_strat));
								
								//add to input
								flags.add(flaginput);
							}
						}
						input.add(flags);
					}
					
				}
				
				//only add to casebase if an state action pair exists
				if(hasInput && hasAction){
					//Case c = new Case(input, action);
					cb.createThenAdd(input, action, stateBasedSim);
					hasInput = false;
					hasAction = false;
				}	
			}
			
			//manual weight selection
			sim_weights.setFeatureWeight("ball", 1);
			sim_weights.setFeatureWeight("goal r", 1);
			sim_weights.setFeatureWeight("goal l", 1);
			sim_weights.setFeatureWeight("flags", 0);
			
			br.close();
			
			System.out.println("CaseBase created.");
			//write to a file
			System.out.println("Writing to file: " + outfile);
			CaseBase.save(cb, outfile);
			System.out.println("Done!");
			
		} catch (FileNotFoundException e) {
			System.out.println(e.toString());
		}
		
	}
	
	
	public static void main(String[] args) {
		File[] files = new File("C:\Users\abeerrafiq\Desktop\GitKrakenRepos\stateBasedKrislet\Parser\Logger").listFiles(new FilenameFilter() {
			public boolean accept(File f, String filename) {
				return filename.endsWith(".lsf");
			}
		});
		LogFile2CaseBase l = new LogFile2CaseBase();
		for (File f: files){
			l.logParser(f.getName(), f.getName().substring(0, f.getName().indexOf(".lsf")) + ".txt");
		}

	}
}
