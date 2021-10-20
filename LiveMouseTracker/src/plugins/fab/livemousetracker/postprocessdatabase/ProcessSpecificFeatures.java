/**
  	@author Fabrice de Chaumont
 	copyright Fabrice de Chaumont @ Institut Pasteur

 	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package plugins.fab.livemousetracker.postprocessdatabase;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import icy.canvas.IcyCanvas;
import icy.image.IcyBufferedImage;
import icy.painter.Painter;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.sequence.Sequence;
import icy.system.profile.Chronometer;
import icy.type.DataType;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.Util;
import plugins.fab.livemousetracker.dataplayer.DBAnimal;
import plugins.fab.livemousetracker.dataplayer.DataBaseRecomputeOnlySpecitifEvents;
import plugins.fab.livemousetracker.dataplayer.DataUtil;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.experiment.Experiment;
import plugins.fab.livemousetracker.liveanalysis.chronogram.Event;
import plugins.fab.livemousetracker.liveanalysis.chronogram.EventTimeLine;
import plugins.fab.livemousetracker.liveanalysis.chronogram.EventType;
import plugins.fab.livemousetracker.liveanalysis.chronogram.TimeLineDataType;
import plugins.fab.livemousetracker.track.TrackContainer;
import plugins.fab.livemousetracker.track.TrackSegment;

public class ProcessSpecificFeatures extends PluginActionable implements PluginThreaded, Painter {

	private static final boolean PROCESS_CONTACT_NEST = false;
	private static final boolean NEST_DENSITY_BINS = false;
	private static final boolean HEATMAP_BASIC = false;
	private static final boolean HEATMAP_CENTERED_ON_ANIMAL_MASS_CENTER = false;
	private static final boolean HEATMAP_CENTERED_ON_ANIMAL_LENGTH = false;
	private static final boolean LOG_IMAGE_RESULT = false;
	private static final boolean DISTANCE_TRAVELLED = false;
	private static final boolean DISTANCE_INTER_ANIMAL = false;
	private static final boolean DISTANCE_MIN_MAX_VS_ALL_OTHERS = false;

	private static final boolean TRIO_GENOTYPE = false;

	private static final boolean TRIO_LEFT_BY_ONE = false;


	private static final boolean CHECK_DATABASE = false;


	private static final boolean RECOMPUTE_GROUP3 = false;
	private static final boolean IN_OUT_GROUP_GROUP3 = false;


	private static final boolean RECOMPUTE_GROUP4 = false;
	private static final boolean IN_OUT_GROUP_GROUP4 = false;



	private static final boolean APPROACH_DIADIC = false;

	private static final boolean TRACKING_QUALITY_EVALUATION_FRAME_DROP = false;

	private static final boolean TRACKING_QUALITY_EVALUATION_ID_NO_NEST_4 = false;
	private static final boolean TRACKING_QUALITY_EVALUATION_RFID_PER_TRACK = false;

	private static final boolean TRACKING_QUALITY_EVALUATION_NB_FRAME_DETECTION_OVER_ALL = false;



	private static final boolean TRACKING_QUALITY_EVALUATION_VERSION_TEST = false;

	private boolean CHECK_DATABASE_MODE_ON = false;

	Connection connection = null;

	ArrayList<AngleMarker> angleMarkerList = new ArrayList<AngleMarker>();

	@Override
	public void run() {

		File files[] = DataUtil.selectDataBaseFiles( this );

		if ( files == null )
		{
			System.out.println("No file selected.");
			return;
		}

		for ( File dataBaseFile : files )
		{
			System.out.println("*************************************************");
			System.out.println("Processing file: " + dataBaseFile.getAbsolutePath() );
			System.out.println("*************************************************");

			connection = null;
			connection = DataUtil.connectDataBase( connection, dataBaseFile );

			if ( CHECK_DATABASE )
			{
				CHECK_DATABASE_MODE_ON = true;
				EventTimeLine timeLine = loadTimeLine( connection, "NEST4" , null , null );
				CHECK_DATABASE_MODE_ON = false;
			}

			if ( TRACKING_QUALITY_EVALUATION_RFID_PER_TRACK )
			{
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				System.out.println( "****");
				System.out.println( "****");
				System.out.println( "****");

				int maxT = 30 * 60 * 60 * 12;
				int window = 30* 60 * 10;

				System.out.println("NUMBER OF RFID CONTROL AND CORRECTION PER TRACK");

				EventTimeLine rfidMisMatch[] = new EventTimeLine[5];
				EventTimeLine rfidMatch[] = new EventTimeLine[5];
				for ( DBAnimal dbAnimal : animalList )
				{
					rfidMatch[dbAnimal.getId()] = loadTimeLine(connection, "RFID MATCH", dbAnimal.getId(), null );
					rfidMisMatch[dbAnimal.getId()] = loadTimeLine(connection, "RFID MISSMATCH", dbAnimal.getId(), null );

					rfidMatch[dbAnimal.getId()].removeEventAfterT( maxT );
					rfidMisMatch[dbAnimal.getId()].removeEventAfterT( maxT );
				}

				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.print( "RFID\t" + dbAnimal.getRFID() + "\tGenotype:\t" + dbAnimal.genotype );
					System.out.print( "\tNumber of match:\t" + rfidMatch[dbAnimal.getId()].getBooleanEventList().size() );
					System.out.println( "\tNumber of mismatch:\t" + rfidMisMatch[dbAnimal.getId()].getBooleanEventList().size() );
				}


			}

			if ( TRACKING_QUALITY_EVALUATION_VERSION_TEST )
			{
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				System.out.println( "****");
				System.out.println( "****");
				System.out.println( "****");


				boolean detectionBoolean[][] = new boolean[5][];
				for ( DBAnimal dbAnimalAlone : animalList )
				{
					detectionBoolean[dbAnimalAlone.getId()] = loadTimeLine(connection, "Detection", dbAnimalAlone.getId(), null ).toBooleanArray();
				}

				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println( "RFID\t" + dbAnimal.getRFID() + "\tGenotype:\t" + dbAnimal.genotype );

					int nbDetectionWithEvent = 0;

					for ( int t = 432000 ; t< 449000 ; t++ )
					{
						if ( detectionBoolean[dbAnimal.getId()][t] )
						{
							nbDetectionWithEvent ++;
						}
					}

					System.out.println("Nb detection with event DETECTION: " + nbDetectionWithEvent );

				}

				TrackContainer trackContainer = DataUtil.loadAnimalData( connection, 432000, 449000 );
				for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
				{
					int animalId = (int) animal.getDataBaseId();

					System.out.println( "RFID:" + animal.getRfidID() );
					int nb=0;
					for ( int t = 432000 ; t< 449000 ; t++ )
					{
						if ( animal.getDetectionAt( t ) != null )
						{
							nb++;
						}
					}
					System.out.println( "number of detection : " + nb );
				}



			}

			if ( TRACKING_QUALITY_EVALUATION_ID_NO_NEST_4 )
			{
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}
				System.out.println( "****");
				System.out.println( "****");
				System.out.println( "****");

				int maxT = 30 * 60 * 60 * 12;
				int window = 30* 60 * 10;

				EventTimeLine detection[] = new EventTimeLine[5];
				for ( DBAnimal dbAnimalAlone : animalList )
				{
					detection[dbAnimalAlone.getId()] = loadTimeLine(connection, "Detection", dbAnimalAlone.getId(), null );
					System.out.println( dbAnimalAlone + " nb event detection: " + detection[dbAnimalAlone.getId()].getBooleanEventList().size() );
					System.out.println( dbAnimalAlone + " total len: " + detection[dbAnimalAlone.getId()].getAllEventLength() );
				}

				System.out.println("NEST4 DENSITY");
				EventTimeLine timeLineNest4 = loadTimeLine( connection, "Nest4" , null , null );

				//EventTimeLine timeLineNest3[] = new EventTimeLine[5];
				boolean timeLineNest3Boolean[][] = new boolean[5][];
				for ( DBAnimal dbAnimalAlone : animalList )
				{
					timeLineNest3Boolean[dbAnimalAlone.getId()] = loadTimeLine( connection, "Nest3" , dbAnimalAlone.getId() , null ).toBooleanArray();
				}

				boolean nest4boolean[] = timeLineNest4.toBooleanArray();
				boolean detectionBoolean[][] = new boolean[5][];
				for ( DBAnimal dbAnimalAlone : animalList )
				{
					detectionBoolean[dbAnimalAlone.getId()] = detection[dbAnimalAlone.getId()].toBooleanArray();
				}



				for ( DBAnimal dbAnimal : animalList )
				{
					int nbDetectionOk = 0;
					int totalFrame = 0;
					for ( int t = 0 ; t< maxT ; t++ )
					//for ( int t = 0 ; t< maxT ; t++ )
					{
						if ( detectionBoolean[dbAnimal.getId()][t] )
						{
							nbDetectionOk ++;
							totalFrame++;
							continue;
						}

						if ( nest4boolean[t] ) continue; // if nest4 -> drop

						boolean inNest3 = false;
						for ( int n = 1 ; n < 5 ; n++ ) // check if any nest3 is having the current animal in it.
						{
//							if ( n!= dbAnimal.getId() ) // don't check over itself.
							if ( timeLineNest3Boolean[n][t] ) inNest3 = true;
						}

						if ( inNest3 ) continue;

						totalFrame++;
					}

					System.out.print( "RFID\t" + dbAnimal.getRFID() + "\tGenotype:\t" + dbAnimal.genotype );
					System.out.println( "\tNumber of detection ok:\t" + nbDetectionOk + "\tover a total of:\t" + totalFrame );
				}
			}

			if ( TRACKING_QUALITY_EVALUATION_FRAME_DROP )
			{
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				System.out.println( "****");
				System.out.println( "****");
				System.out.println( "****");

				int maxT = 30 * 60 * 60 * 12;
				int window = 30* 60 * 10;

				System.out.println("FRAME DROP");

				EventTimeLine frameDrop = loadTimeLine(connection, "FRAME DROP", null, null );

				System.out.println( "Total: " + frameDrop.getAllEventLength() );
				System.out.println( "Nb Events: " + frameDrop.getBooleanEventList().size() );

			}

			if ( TRACKING_QUALITY_EVALUATION_NB_FRAME_DETECTION_OVER_ALL )
			{
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
				}

				System.out.println( "****");
				System.out.println( "****");
				System.out.println( "****");

				int maxT = 30 * 60 * 60 * 12;
				int window = 30* 60 * 10;

				System.out.println("DETECTION");
				EventTimeLine detection[] = new EventTimeLine[5];
				for ( DBAnimal dbAnimalAlone : animalList )
				{
					detection[dbAnimalAlone.getId()] = loadTimeLine(connection, "Detection", dbAnimalAlone.getId(), null );
				}
				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );

					for ( int tt=0; tt<maxT; tt+=window )
					{
						int nbOk = 0;
						for ( int t = tt ; t < tt+window ; t++ )
						{
							if( detection[dbAnimal.getId()].eventPresentAt( t ) )
							{
								nbOk++;
							}
						}
						System.out.println(nbOk);
					}
				}

//				System.out.println("RFID MISMATCH");
//				EventTimeLine rfidMisMatch[] = new EventTimeLine[5];
//				for ( DBAnimal dbAnimal : animalList )
//				{
//					rfidMisMatch[dbAnimal.getId()] = loadTimeLine(connection, "RFID MISMATCH", dbAnimal.getId(), null );
//				}
//
//				for ( DBAnimal dbAnimal : animalList )
//				{
//					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
//
//					for ( int tt=0; tt<maxT; tt+=window )
//					{
//						int nbEvent = 0;
//						for ( int t = tt ; t < tt+window ; t++ )
//						{
//							if( rfidMisMatch[dbAnimal.getId()].eventPresentAt( t ) )
//							{
//								nbEvent++;
//							}
//						}
//						System.out.println(nbEvent);
//					}
//				}
//
//				System.out.println("RFID ASSIGN ANONYMOUS TRACK");
//				EventTimeLine rfidAssignAnonymous[] = new EventTimeLine[5];
//				for ( DBAnimal dbAnimal : animalList )
//				{
//					rfidAssignAnonymous[dbAnimal.getId()] = loadTimeLine(connection, "RFID ASSIGN ANONYMOUS TRACK", dbAnimal.getId(), null );
//				}
//
//				for ( DBAnimal dbAnimal : animalList )
//				{
//					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
//
//					for ( int tt=0; tt<maxT; tt+=window )
//					{
//						int nbEvent = 0;
//						for ( int t = tt ; t < tt+window ; t++ )
//						{
//							if( rfidAssignAnonymous[dbAnimal.getId()].eventPresentAt( t ) )
//							{
//								nbEvent++;
//							}
//						}
//						System.out.println(nbEvent);
//					}
//				}
//
//				System.out.println("MACHINE LEARNING ASSOCIATION");
//				EventTimeLine machineLearningAssociation[] = new EventTimeLine[5];
//				for ( DBAnimal dbAnimal : animalList )
//				{
//					machineLearningAssociation[dbAnimal.getId()] = loadTimeLine(connection, "MACHINE LEARNING ASSOCIATION", dbAnimal.getId(), null );
//				}
//
//				for ( DBAnimal dbAnimal : animalList )
//				{
//					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
//
//					for ( int tt=0; tt<maxT; tt+=window )
//					{
//						int nbEvent = 0;
//						for ( int t = tt ; t < tt+window ; t++ )
//						{
//							if( machineLearningAssociation[dbAnimal.getId()].eventPresentAt( t ) )
//							{
//								nbEvent++;
//							}
//						}
//						System.out.println(nbEvent);
//					}
//				}
//
//
//				System.out.println("FRAME DROP");
//				EventTimeLine frameDrop[] = new EventTimeLine[5];
//				for ( DBAnimal dbAnimal : animalList )
//				{
//					frameDrop[dbAnimal.getId()] = loadTimeLine(connection, "FRAME DROP", dbAnimal.getId(), null );
//				}
//
//				for ( DBAnimal dbAnimal : animalList )
//				{
//					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
//
//					for ( int tt=0; tt<maxT; tt+=window )
//					{
//						int nbEvent = 0;
//						for ( int t = tt ; t < tt+window ; t++ )
//						{
//							if( frameDrop[dbAnimal.getId()].eventPresentAt( t ) )
//							{
//								nbEvent++;
//							}
//						}
//						System.out.println(nbEvent);
//					}
//				}


			}

			if ( APPROACH_DIADIC )
			{
				// contacts pris en compte
				// 10 frame ou l'animal est libre et en approach
				// 2 animaux > lesquel et qui a approché l'autre.
				// pas de stop
				// contact < 30 supprimés

				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
				}

				System.out.println( "****");
				System.out.println( "****");
				System.out.println( "****");

				int maxT = 30 * 60 * 60 * 12;
				int window = 30* 60 * 10;

				System.out.println("DETECTION");
				EventTimeLine detection[] = new EventTimeLine[5];

				for ( DBAnimal dbAnimalAlone : animalList )
				{
					detection[dbAnimalAlone.getId()] = loadTimeLine(connection, "Detection", dbAnimalAlone.getId(), null );
				}

				// load contact for each pair
				EventTimeLine contact[][] = new EventTimeLine[5][5];
				for ( DBAnimal dbAnimalA : animalList )
				{
					for ( DBAnimal dbAnimalB : animalList )
					{
						contact[dbAnimalA.getId()][dbAnimalB.getId()]
								= loadTimeLine( connection, "Contact" , dbAnimalA.getId() , dbAnimalB.getId() );
					}
				}

				// load approach for each pair
				EventTimeLine approach[][] = new EventTimeLine[5][5];
				for ( DBAnimal dbAnimalA : animalList )
				{
					for ( DBAnimal dbAnimalB : animalList )
					{
						approach[dbAnimalA.getId()][dbAnimalB.getId()]
								= loadTimeLine( connection, "Approach" , dbAnimalA.getId() , dbAnimalB.getId() );
					}
				}

				// load escape for each pair
				EventTimeLine escape[][] = new EventTimeLine[5][5];
				for ( DBAnimal dbAnimalA : animalList )
				{
					for ( DBAnimal dbAnimalB : animalList )
					{
						escape[dbAnimalA.getId()][dbAnimalB.getId()]
								= loadTimeLine( connection, "Escape" , dbAnimalA.getId() , dbAnimalB.getId() );
					}
				}

				int resultApproach[][] = new int[5][5];
				int resultLeave[][] = new int[5][5];

				for ( int idA = 1 ; idA < 5 ; idA++ )
				{
					for ( int idB = idA+1 ; idB < 5 ; idB++ )
					{
						for ( Event contactEvent : contact[idA][idB].getBooleanEventList() )
						{
							if( contactEvent.getLength() < 30 ) continue;

							int start = contactEvent.getStartFrame() -1 ;

							if ( approach[idA][idB].eventPresentAt( start ) )
							{
								resultApproach[idA][idB]++;
							}

							if ( approach[idB][idA].eventPresentAt( start ) )
							{
								resultApproach[idB][idA]++;
							}

							int end = contactEvent.getStartFrame() +1 ;

							if ( escape[idA][idB].eventPresentAt( end ) )
							{
								resultLeave[idA][idB]++;
							}

							if ( escape[idB][idA].eventPresentAt( end ) )
							{
								resultLeave[idB][idA]++;
							}

						}
					}
				}

				System.out.println("Approach");
				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
				}

				for ( DBAnimal dbAnimalA : animalList )
				{
					for ( DBAnimal dbAnimalB : animalList )
					{
						System.out.print( resultApproach[dbAnimalA.getId()][dbAnimalB.getId()] + "\t");
					}
					System.out.println("");
				}

				System.out.println("Leave");
				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
				}
				for ( DBAnimal dbAnimalA : animalList )
				{
					for ( DBAnimal dbAnimalB : animalList )
					{
						System.out.print( resultLeave[dbAnimalA.getId()][dbAnimalB.getId()] + "\t");
					}
					System.out.println("");
				}


			}

			if ( RECOMPUTE_GROUP4 )
			{
				// all animals detected
				// no stop story

				// delete all previous events group4

				DataBaseRecomputeOnlySpecitifEvents.delEvents(connection , "Group4" );

				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				EventTimeLine notInContact[] = new EventTimeLine[5]; // notTnContact with anybody
				EventTimeLine inContact[] = new EventTimeLine[5]; // inContact with anybody
				EventTimeLine detection[] = new EventTimeLine[5];

				int maxT = 0;
				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
					notInContact[dbAnimal.getId()] = loadTimeLine( connection, "Contact" , dbAnimal.getId() , null , true );
					inContact[dbAnimal.getId()] = loadTimeLine( connection, "Contact" , dbAnimal.getId() , null );
					detection[dbAnimal.getId()] = loadTimeLine(connection, "Detection", dbAnimal.getId(), null );
					maxT = Math.max( maxT, detection[dbAnimal.getId()].getMaxT() );
				}

				// load contact for each pair
				EventTimeLine contact[][] = new EventTimeLine[5][5];
				for ( DBAnimal dbAnimal : animalList )
				{
					for ( DBAnimal dbAnimal2 : animalList )
					{
						contact[dbAnimal.getId()][dbAnimal2.getId()]
								= loadTimeLine( connection, "Contact" , dbAnimal.getId() , dbAnimal2.getId() );
					}
				}

				System.out.println("MaxT = " + maxT );
				EventTimeLine group4 = new EventTimeLine("Group4", TimeLineDataType.BOOLEAN );

				tLoop:
				for ( int t = 0 ; t < maxT  ; t++ )
				{
					if ( t%10000 == 0 )
					{
						System.out.println( t + " / " + maxT );
					}

					for ( int animal=1; animal < 5 ; animal++ )
					{
						if( !inContact[animal].eventPresentAt( t ) ) continue tLoop;
					}

					// check if one animal has 2 contact at least.
					// in that case they are all in contact
					for ( int animal=1; animal < 5 ; animal++ )
					{
						int count = 0;
						for ( int animal2=1; animal2 < 5 ; animal2++ )
						{
							if ( contact[animal][animal2].eventPresentAt( t ) ) count++;
							if ( count > 1 )
							{
								group4.addPunctualEvent( t );
								continue tLoop;
							}
						}
					}

					/*
					EventTimeLine notInContactTL = notInContact[dbAnimalAlone.getId()];

					int nbEvent = notInContactTL.getBooleanEventList().size();
					int nb = 0;
					for ( Event notInC : notInContactTL.getBooleanEventList() )
					{
						nb++;
						if ( nb%100 == 0 )
						{
							System.out.println( nb + " / " + nbEvent );
						}
						for ( int t = notInC.getStartFrame() ; t <= notInC.getEndFrame() ; t++ )
						{

							// check if the tested animal is in contact.
							if ( inContact[dbAnimalAlone.getId()].eventPresentAt( t ) ) continue;
							// check detection presence.
							if ( !detection[dbAnimalAlone.getId()].eventPresentAt( t ) ) continue;

							// check if all other 3 are in contact together. (no need to check the together indeed... :)

							boolean contactOk = true;
							for ( DBAnimal dbAnimalTest : animalList )
							{
								if ( dbAnimalAlone == dbAnimalTest ) continue;

								if ( !(inContact[dbAnimalTest.getId()].eventPresentAt( t )
										&& detection[dbAnimalTest.getId()].eventPresentAt( t ) ) )
								{
									contactOk = false;
								}
							}
							if ( contactOk )
							{
								group4.addPunctualEvent( t );
							}


						}

					}
*/

				}

				System.out.println("Saving...");
				Experiment.saveEventTimeLine(
						connection , group4 , "Group4" , "group" , null , null, null, null );





			}


			if ( RECOMPUTE_GROUP3 )
			{
				// all animals detected
				// no stop story
				// 1 animal isolated

				// delete all previous events group3

				DataBaseRecomputeOnlySpecitifEvents.delEvents(connection , "Group3" );

				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				EventTimeLine notInContact[] = new EventTimeLine[5]; // notTnContact with anybody
				EventTimeLine inContact[] = new EventTimeLine[5]; // inContact with anybody
				EventTimeLine detection[] = new EventTimeLine[5];

				int maxT = 0;
				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
					notInContact[dbAnimal.getId()] = loadTimeLine( connection, "Contact" , dbAnimal.getId() , null , true );
					inContact[dbAnimal.getId()] = loadTimeLine( connection, "Contact" , dbAnimal.getId() , null );
					detection[dbAnimal.getId()] = loadTimeLine(connection, "Detection", dbAnimal.getId(), null );
					maxT = Math.max( maxT, detection[dbAnimal.getId()].getMaxT() );
				}

				System.out.println("MaxT = " + maxT );

				for ( DBAnimal dbAnimalAlone : animalList )
				{
					System.out.println("processing animal " + dbAnimalAlone.toString() );
					EventTimeLine group3 = new EventTimeLine("Group3", TimeLineDataType.BOOLEAN );

					EventTimeLine notInContactTL = notInContact[dbAnimalAlone.getId()];

					int nbEvent = notInContactTL.getBooleanEventList().size();
					int nb = 0;
					for ( Event notInC : notInContactTL.getBooleanEventList() )
					{
						nb++;
						if ( nb%100 == 0 )
						{
							System.out.println( nb + " / " + nbEvent );
						}
						for ( int t = notInC.getStartFrame() ; t <= notInC.getEndFrame() ; t++ )
						{

							// check if the tested animal is in contact.
							if ( inContact[dbAnimalAlone.getId()].eventPresentAt( t ) ) continue;
							// check detection presence.
							if ( !detection[dbAnimalAlone.getId()].eventPresentAt( t ) ) continue;

							// check if all other 3 are in contact together. (no need to check the together indeed... :)

							boolean contactOk = true;
							for ( DBAnimal dbAnimalTest : animalList )
							{
								if ( dbAnimalAlone == dbAnimalTest ) continue;

								if ( !(inContact[dbAnimalTest.getId()].eventPresentAt( t )
										&& detection[dbAnimalTest.getId()].eventPresentAt( t ) ) )
								{
									contactOk = false;
								}
							}
							if ( contactOk )
							{
								group3.addPunctualEvent( t );
							}


						}

					}

					System.out.println("Saving...");
					Experiment.saveEventTimeLine(
							connection , group3 , "Group3" , "group" , dbAnimalAlone.getId() , null, null, null );

				}

			}


			if ( TRIO_GENOTYPE )
			{
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

//				EventTimeLine[] contactEvent = new EventTimeLine[5];

				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
//					contactEvent[dbAnimal.getId()] = loadTimeLine(connection, "Contact", dbAnimal.getId(), null );
				}

				System.out.println( "****");
				System.out.println( "****");
				System.out.println( "****");

				int maxT = 30 * 60 * 60 * 12;
				int window = 30 * 60 * 10;

				for ( DBAnimal dbAnimal : animalList )
				{
//					nest3Event[dbAnimal.getId()]
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
					EventTimeLine nest3 = loadTimeLine( connection, "Nest3" , dbAnimal.getId() , null );

					for ( int t = 0 ; t < maxT ; t+=window )
					{
						int nb = nest3.getNbEvent( t , t+window );
						int totalDuration = nest3.getEventLenght( t , t+window );
						System.out.println(""+nb+"\t"+totalDuration);
					}

				}

			}


			if ( NEST_DENSITY_BINS )
			{
				{
					System.out.println("NEST4 DENSITY");
					EventTimeLine timeLineNest4 = loadTimeLine( connection, "Nest4" , null , null );
					DecimalFormat dc = new DecimalFormat( "#.##");
					int window = 30 * 60 * 10; // 10 minutes
					for ( int t =0 ; t < timeLineNest4.getMaxT() ; t+=window )
					{
						System.out.println(
								dc.format( timeLineNest4.getDensity( t , t + window ) )
								);
					}
					System.out.println("nb event: " + timeLineNest4.getNbEvent( 0 , Integer.MAX_VALUE ));
					System.out.println("total len: " + timeLineNest4.getAllEventLength() );
				}

				for ( int id=1 ; id<5; id ++ )
				{
					System.out.println("NEST3 DENSITY");
					EventTimeLine timeLineNest3 = loadTimeLine( connection, "Nest3" , id , null );
					DecimalFormat dc = new DecimalFormat( "#.##");
					int window = 30 * 60 * 10; // 10 minutes
					for ( int t =0 ; t < timeLineNest3.getMaxT() ; t+=window )
					{
						System.out.println(
								dc.format( timeLineNest3.getDensity( t , t + window ) )
								);
					}
					System.out.println("nb event: " + timeLineNest3.getNbEvent( 0 , Integer.MAX_VALUE ));
					System.out.println("total len: " + timeLineNest3.getAllEventLength() );
				}

			}

			if ( HEATMAP_BASIC ) // A etendre sur location ?
			{
				int timePoint = 0;
				Sequence mapSequence = new Sequence("HeatMap");
				//IcyBufferedImage image = new IcyBufferedImage(1000, 1000, 4, DataType.FLOAT );
				addSequence( mapSequence );

				int maxT = DataUtil.getMaxNumberOfFrame(connection);
				int window = 30*60*10; // 10 minutes
				for ( int t = 0 ; t < maxT ; t+=window )
				{
					System.out.println("Computing t:" + t + " over " + maxT );
					IcyBufferedImage image = new IcyBufferedImage( 512, 424, 4, DataType.FLOAT );
					mapSequence.setImage( timePoint , 0, image );

					TrackContainer trackContainer = DataUtil.loadAnimalData( connection, t, t+window );
					for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
					{
						int animalId = (int) animal.getDataBaseId();

						float[] imageBuffer = image.getDataXYAsFloat( animalId-1 );

						for ( TrackSegment ts : animal.getTrackSegments() )
						{
							for ( MouseDetection d : ts.getDetectionList() )
							{
								for ( Point p : d.getROI2DArea().getBooleanMask( true ).getPoints() )
								{
									int offset = p.x + p.y * 512; // 1000
									imageBuffer[offset]++;
								}
							}
						}
					}
					image.dataChanged();
					timePoint++;
				}

//				mapSequence.addImage( image );
//				addSequence( mapSequence );
				System.out.println("Heat Map : Finished");
			}

			//		{
			//			AffineTransform transform = new AffineTransform();
			//			transform.setToIdentity();
			//			transform.rotate( Math.PI / 10d );
			//
			//			Point2D p = new Point2D.Double( 10 ,10 );
			//			System.out.println( transform.transform( p , p ) );
			//		}

			if ( DISTANCE_MIN_MAX_VS_ALL_OTHERS )
			{
				SimpleAnimalLocation []sa = new SimpleAnimalLocation[5];

				for ( int id=1 ; id<5; id ++ )
				{
					sa[id] =  new SimpleAnimalLocation( connection , id );
				}

				int maxT = DataUtil.getMaxNumberOfFrame(connection);


				int window = 30*60*1; //  minutes

				for ( int t = 0 ; t < maxT ; t+=window )
				{
					for ( int id1=1 ; id1<5; id1 ++ )
					{
						double minDistance = Double.MAX_VALUE;
						double maxDistance = Double.MIN_NORMAL;
						int nbVal = 0;
						for ( int tt = t; tt < t+window ; tt++ )
						{
							for ( int id2=1 ; id2<5; id2 ++ )
							{
								if ( id1 == id2 ) continue;

								Point2D p1 = sa[id1].getPoint( tt );
								Point2D p2 = sa[id2].getPoint( tt );

								if ( p1 == null ) continue;
								if ( p2 == null ) continue;

								double distance = p1.distance( p2 );

								if ( distance < minDistance )
								{
									minDistance = distance;
								}
								if ( distance > maxDistance )
								{
									maxDistance = distance;
								}
								nbVal++;

							}

						}

						if ( nbVal == 0 )
						{
							maxDistance = -1;
							minDistance = -1;
						}

						System.out.print( (int)minDistance + "\t" );
					}
					System.out.println("");
				}

			}

			if ( DISTANCE_INTER_ANIMAL )
			{
				SimpleAnimalLocation []sa = new SimpleAnimalLocation[5];

				for ( int id=1 ; id<5; id ++ )
				{
					sa[id] =  new SimpleAnimalLocation( connection , id );
				}

				int maxT = DataUtil.getMaxNumberOfFrame(connection);

				for ( int id1=1 ; id1<5; id1 ++ )
				{
					for ( int id2=1 ; id2<5; id2 ++ )
					{
						if ( id1 == id2 ) continue;

						System.out.println("****");
						System.out.println("*******");
						System.out.println("****************");
						System.out.println("**************************");
						System.out.println("***************************************");
						System.out.println("Distance between " + sa[id1].id + " and " + sa[id2].id );
						System.out.println("min\tmax\tmean\tstd");

						//int window = 30*60*5; //  minutes
						int window = 30*60*1; //  minutes

						ArrayList<Double> valueList_all = new ArrayList<Double>();
						double sumDistance_all = 0;
						int nbVal_all = 0;

						for ( int t = 0 ; t < maxT ; t+=window )
						{
							double minDistance = Double.MAX_VALUE;
							double maxDistance = Double.MIN_NORMAL;
							int nbVal = 0;
							double sumDistance = 0;
							ArrayList<Double> valueList = new ArrayList<Double>();

							for ( int tt = t; tt < t+window ; tt++ )
							{
								Point2D p1 = sa[id1].getPoint( tt );
								Point2D p2 = sa[id2].getPoint( tt );

								if ( p1 == null ) continue;
								if ( p2 == null ) continue;

								double distance = p1.distance( p2 );

								if ( distance < minDistance )
								{
									minDistance = distance;
								}
								if ( distance > maxDistance )
								{
									maxDistance = distance;
								}

								// local

								valueList.add( distance );
								sumDistance+= distance;
								nbVal++;

								// all

								valueList_all.add( distance );
								sumDistance_all += distance;
								nbVal_all ++;

							}

							double meanDistance = 0;
							double stdDev = 0;
							if ( nbVal > 0 )
							{
								meanDistance = sumDistance / nbVal;
								double[] vals = new double[ valueList.size() ];
								for ( int i = 0 ; i < valueList.size(); i ++ ){
									vals[i] = valueList.get( i );
								}
								stdDev = flanagan.analysis.Stat.standardDeviation( vals );
							}

							if ( nbVal == 0 )
							{
								minDistance = -1;
								maxDistance = -1;
							}

							System.out.print( minDistance );
							System.out.print( "\t" );
							System.out.print( maxDistance );
							System.out.print( "\t" );
							System.out.print( meanDistance );
							System.out.print( "\t" );
							System.out.print( stdDev );
							System.out.println( "" );

						}

						if ( nbVal_all > 0 )
						{
							double meanDistance_all = sumDistance_all / nbVal_all;
							double[] vals = new double[ valueList_all.size() ];
							for ( int i = 0 ; i < valueList_all.size(); i ++ ){
								vals[i] = valueList_all.get( i );
							}
							double stdDev = flanagan.analysis.Stat.standardDeviation( vals );

							System.out.println("mean dist all : " + meanDistance_all );
							System.out.println("std dev: " + stdDev );

						}
					}
				}

			}


			if ( DISTANCE_TRAVELLED )
			{
				SimpleAnimalLocation []sa = new SimpleAnimalLocation[5];

				for ( int id=1 ; id<5; id ++ )
				{
					sa[id] =  new SimpleAnimalLocation( connection , id );
				}


				int maxT = DataUtil.getMaxNumberOfFrame(connection);

				for ( int id=1 ; id<5; id ++ )
				{

					double totalDistance = 0;
					int window = 30*60*5; //  minutes

					System.out.println("*****animal id: " + sa[id].id );
					for ( int t = 0 ; t < maxT ; t+=window )
					{
						for ( int tt = t; tt < t+window ; tt++ )
						{
							Point2D prev = sa[id].getPoint( tt-3 );
							Point2D next = sa[id].getPoint( tt+3 );

							if ( prev == null ) continue;
							if ( next == null ) continue;

							double distance = prev.distance( next ) / 7d;

							totalDistance+=distance;
						}
						System.out.println( totalDistance );
					}
				}

			}


			/** take an animal and plot the others animal by re rotating the animal and position it at the center*/
			if ( HEATMAP_CENTERED_ON_ANIMAL_MASS_CENTER )
			{
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				// load NEST4
				EventTimeLine maskNest4= null;
				System.out.println("Loading events #" + maskNest4 );
				maskNest4 = loadTimeLine( connection, "Nest4" , null , null );
				System.out.println( "Nb events loaded : " + maskNest4.getNbEvent( 0 , Integer.MAX_VALUE ) );

				// load NEST3
				EventTimeLine maskNest3= null;
				System.out.println("Loading events #" + maskNest3 );
				maskNest3 = loadTimeLine( connection, "Nest3" , null , null );
				System.out.println( "Nb events loaded : " + maskNest3.getNbEvent( 0 , Integer.MAX_VALUE ) );

				// load stops
				EventTimeLine maskStop[]= new EventTimeLine[animalList.size()+1];
				for ( int id = 1 ; id < animalList.size()+1 ; id++ )
				{
//					if (id == idAnimalToCheck ) continue;
					System.out.println("Loading events #" + id );
					//maskStop[id] = loadTimeLine( connection, "Stop" , idAnimalToCheck, null );
					maskStop[id] = loadTimeLine( connection, "Stop" , id, null );
					System.out.println( "Nb events loaded : " + maskStop[id].getNbEvent( 0 , Integer.MAX_VALUE ) );
				}


				for ( int idAnimalToCheck=1 ; idAnimalToCheck< animalList.size()+1; idAnimalToCheck ++ )
				{
					Chronometer animalComputation = new Chronometer("Animal computation");

					//int idAnimalToCheck=1;
					System.out.println("ANIMAL TO CHECK ID = " + idAnimalToCheck );
//					Sequence mapPolarSequence = new Sequence("HeatMap Polar " + dataBaseFile.getName() + " id " + idAnimalToCheck );
					//				mapPolarSequence.addPainter( this );
//					addSequence( mapPolarSequence );

//					Sequence mapPlanarSequence = new Sequence("HeatMap Planar id " + idAnimalToCheck );
//					addSequence( mapPlanarSequence );


					int minT = 0;
					//int maxT = DataUtil.getMaxNumberOfFrame(connection);
					int maxT = 30*60 * 60 * 12;

					//			minT = (int)(30*60*60*8.25f);	// minT
					//			maxT = minT+ 30*60*60;			// override

					//			minT = 0;
					//			maxT = 30*60*10;

					//			minT = (int)(30*60*60*14.15f);	// minT
					//			maxT = minT+ 30*60*10;			// override

					int window = 30*60*5; //  minutes

					// Load masking time lines.

					// load approaches
					EventTimeLine maskApproach[]= new EventTimeLine[animalList.size()+1];
					for ( int id = 1 ; id < animalList.size()+1 ; id++ )
					{
						if (id == idAnimalToCheck ) continue;
						System.out.println("Loading events #" + id );
						maskApproach[id] = loadTimeLine( connection, "Approach" , id, idAnimalToCheck );
						System.out.println( "Nb events loaded : " + maskApproach[id].getNbEvent( 0 , Integer.MAX_VALUE ) );
					}
					// load contacts
					EventTimeLine maskContact[]= new EventTimeLine[animalList.size()+1];
					for ( int id = 1 ; id < animalList.size()+1 ; id++ )
					{
						if (id == idAnimalToCheck ) continue;
						System.out.println("Loading events #" + id );
						maskContact[id] = loadTimeLine( connection, "Contact" , id, idAnimalToCheck );
						System.out.println( "Nb events loaded : " + maskContact[id].getNbEvent( 0 , Integer.MAX_VALUE ) );
					}

					System.out.println("Masking timeline loaded");

					// One BIG Image
//					IcyBufferedImage imagePolar = new IcyBufferedImage(1000, 1000, animalList.size(), DataType.FLOAT );
//					mapPolarSequence.setImage( 0 , 0 , imagePolar );

//					IcyBufferedImage imagePlanar = new IcyBufferedImage(1000, 1000, animalList.size(), DataType.FLOAT );
//					mapPlanarSequence.setImage( 0 , 0 , imagePlanar );

					int[][] angleApproach = new int[animalList.size()+1][360];

					for ( int t = minT ; t < maxT ; t+=window )
						//int t = 0;
					{
						System.out.println( (float)( t - minT ) * ( 100f / ( maxT - minT ) ) + " % --- maxT: " + maxT );

//						double nest4Density = maskNest4.getDensity( t , t + window );
//						System.out.println("Mask 4 density: " + nest4Density );
//						if( nest4Density > 0.95 )
//						{
//							System.out.println("Skip nest4");
//							continue;
//						}

						//				if( maskNest4.eventPresentAt( t , t+window ) ) { System.out.println("Skip nest4"); continue; }
						//				if( maskNest3.eventPresentAt( t , t+window ) ) { System.out.println("Skip nest3"); continue; }

						int nbAnimalDrawn = 0;
						int nbAngleAffected =0;
						//				IcyBufferedImage image = new IcyBufferedImage(1000, 1000, 4, DataType.FLOAT );
						//				mapSequence.setImage( mapSequence.getSizeT() , 0 , image );

//						Chronometer loadAnimalDataChrono = new Chronometer("Load animal data chrono");


						TrackContainer trackContainer = DataUtil.loadAnimalData( connection, t, t+window );

//						loadAnimalDataChrono.displayInSeconds();

						Animal studiedAnimal = trackContainer.getAnimalWithDataBaseId( idAnimalToCheck );
//						System.out.println("Working on t : " + t + "/" + (int)(t+window) );

						AffineTransform transform = new AffineTransform();

						for ( int tt = t ; tt < t+window; tt++ )
						{
							if( maskNest4.eventPresentAt( tt ) ) continue;
							if( maskNest3.eventPresentAt( tt ) ) continue;
							if( !maskStop[idAnimalToCheck].eventPresentAt( tt ) ) continue;

							MouseDetection studiedDetection = studiedAnimal.getDetectionAt(tt);
							if ( studiedDetection == null ) continue;

							transform.setToIdentity();
							transform.rotate( -studiedDetection.angle );//- Math.PI / 2d );

							for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
							{
								if ( animal == studiedAnimal ) continue;

								Event approach = maskApproach[(int) (animal.getDataBaseId())].getEventAt( tt );
								if ( approach != null )
									if( approach.getLength() > 30 ) // Approaches must be greater than 1 sec !
									{
										int nbContactAtEnd = maskContact[(int) (animal.getDataBaseId())].getNbEvent( approach.getEndFrame()-1, approach.getEndFrame()+1 );
										if( nbContactAtEnd > 0 )
										{
//											float[] imagePolarBuffer = imagePolar.getDataXYAsFloat( (int) (animal.getDataBaseId()-1) );
//											float[] imagePlanarBuffer = imagePlanar.getDataXYAsFloat( (int) (animal.getDataBaseId()-1) );

											MouseDetection detection = animal.getDetectionAt( tt );
											if ( detection != null )
											{
												// check final angle.
												{
													if ( approach.getEndFrame() == tt ) // about the final angle
													{
														// draw the studiedMouse in planar
														{
//															float[] imagePlanarBufferStudied = imagePlanar.getDataXYAsFloat( (int) (studiedAnimal.getDataBaseId()-1) );
//															for ( Point p : studiedDetection.getROI2DArea().getBooleanMask( true ).getPoints() )
//															{
//																int offsetPlanarStudied = (int) ( (int)p.getX() + (int)(p.getY()) * 1000);
//																imagePlanarBufferStudied[offsetPlanarStudied]++;
//															}

														}

														MouseDetection detectionPrevious = animal.getDetectionAt( tt );
														// transform mass center of incoming animal
														Point2D p = detectionPrevious.getMassCenter().toPoint2D();
														p.setLocation( p.getX() - studiedDetection.getMassCenter().getX() , p.getY() - studiedDetection.getMassCenter().getY() );
														Point2D transformedPoint = transform.transform( p , null );
														//transformedPoint.setLocation( transformedPoint.getX()+500, transformedPoint.getY() + 500 );
														int angleBetweenAnimals = (int) Math.toDegrees( Math.atan2( transformedPoint.getY(), transformedPoint.getX() ) );
														if ( angleBetweenAnimals < 0 ) angleBetweenAnimals+=360;
														angleApproach[(int)animal.getDataBaseId()][ angleBetweenAnimals ]++;
														nbAngleAffected++;
														transformedPoint.setLocation( transformedPoint.getX()+500, transformedPoint.getY() + 500 );
														angleMarkerList.add( new AngleMarker( new Point2D.Double( 500 , 500 ),
																transformedPoint ,angleBetweenAnimals , (int)animal.getDataBaseId() ) );

													}
												}

												/*
												if ( approach.getEndFrame() == tt )
												{
													nbAnimalDrawn ++;
													for ( Point p : detection.getROI2DArea().getBooleanMask( true ).getPoints() )
													{
														// planar
														int offsetPlanar = (int) ( (int)p.getX() + (int)(p.getY()) * 1000);
//														imagePlanarBuffer[offsetPlanar]++;
														// polar
														p.setLocation( p.getX() - studiedDetection.getMassCenter().getX() , p.getY() - studiedDetection.getMassCenter().getY() );
														Point2D transformedPoint = transform.transform( p , null );
														transformedPoint.setLocation( transformedPoint.getX()+500, transformedPoint.getY() + 500 );

														if ( transformedPoint.getX() > 0 && transformedPoint.getY() > 0 &&
																transformedPoint.getX() < 1000 && transformedPoint.getY() < 1000 )
														{
															// plot animal on heatmap
//															int offsetPolar = (int) ( (int)transformedPoint.getX() + (int)(transformedPoint.getY()) * 1000);
//															imagePolarBuffer[offsetPolar]++;
														}
													}
												}
												*/
											}

										}
									}
							}

						}
//						imagePolar.dataChanged();
//						imagePlanar.dataChanged();

						// name channels
						{
							for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
							{
//								mapPolarSequence.setChannelName( (int)(animal.getDataBaseId()-1) , animal.getRfidID().substring( animal.getRfidID().length()-4 ) );
							}
						}
//						System.out.println("Nb Animal drawn: " + nbAnimalDrawn );
//						System.out.println("Nb angle affected: " + nbAngleAffected );

					}

					System.out.println("Id1\tId2\tId3\tId4" );
					for ( int a = 0; a < 360; a++ )
					{
						for ( int id = 1 ; id < animalList.size()+1 ; id++)
						{
							System.out.print( angleApproach[id][a]+"\t" );
						}
						System.out.println("");
					}


					// Log image

					if ( LOG_IMAGE_RESULT )
					{
//						for ( int t = 0 ; t < mapPolarSequence.getSizeT() ; t++ )
//						{
//							IcyBufferedImage imageToLog = mapPolarSequence.getImage( t , 0 );
//							for ( int c = 0 ; c<imageToLog.getSizeC() ; c++ )
//							{
//								float [] data = imageToLog.getDataXYAsFloat( c );
//								for ( int i = 0 ; i< data.length ; i++ )
//								{
//									float value = (float)Math.log10( data[i] );
//									if ( value < 0 ) value = 0;
//									data[i] = value;
//								}
//							}
//							imageToLog.dataChanged();
//						}
					}
					animalComputation.displayInSeconds();
				}
				System.out.println("Heat Map : Finished");
			}



			/** take an animal and plot the others animal by re rotating the animal and position it at the center*/
			if ( HEATMAP_CENTERED_ON_ANIMAL_LENGTH )
			{
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				// load stops
				EventTimeLine maskStop[]= new EventTimeLine[animalList.size()+1];
				for ( int id = 1 ; id < animalList.size()+1 ; id++ )
				{
//					if (id == idAnimalToCheck ) continue;
					System.out.println("Loading events #" + id );
					//maskStop[id] = loadTimeLine( connection, "Stop" , idAnimalToCheck, null );
					maskStop[id] = loadTimeLine( connection, "Stop" , id, null );
					System.out.println( "Nb events loaded : " + maskStop[id].getNbEvent( 0 , Integer.MAX_VALUE ) );
				}

				for ( int idAnimalToCheck=1 ; idAnimalToCheck< animalList.size()+1; idAnimalToCheck ++ )
				{
					Chronometer animalComputation = new Chronometer("Animal computation");

					//int idAnimalToCheck=1;
					System.out.println("ANIMAL TO CHECK ID = " + idAnimalToCheck );
//					Sequence mapPolarSequence = new Sequence("HeatMap Polar " + dataBaseFile.getName() + " id " + idAnimalToCheck );
					//				mapPolarSequence.addPainter( this );
//					addSequence( mapPolarSequence );

//					Sequence mapPlanarSequence = new Sequence("HeatMap Planar id " + idAnimalToCheck );
//					addSequence( mapPlanarSequence );


					int minT = 0;
					//int maxT = DataUtil.getMaxNumberOfFrame(connection);
//					int maxT = 30*60 * 60 * 12;
					int maxT = 30*60 * 20;

					//			minT = (int)(30*60*60*8.25f);	// minT
					//			maxT = minT+ 30*60*60;			// override

					//			minT = 0;
					//			maxT = 30*60*10;

					//			minT = (int)(30*60*60*14.15f);	// minT
					//			maxT = minT+ 30*60*10;			// override

					int window = 30*60*5; //  minutes

					// Load masking time lines.

					// load approaches
					EventTimeLine maskApproach[]= new EventTimeLine[animalList.size()+1];
					for ( int id = 1 ; id < animalList.size()+1 ; id++ )
					{
						if (id == idAnimalToCheck ) continue;
						System.out.println("Loading events #" + id );
						maskApproach[id] = loadTimeLine( connection, "Approach" , id, idAnimalToCheck );
						System.out.println( "Nb events loaded : " + maskApproach[id].getNbEvent( 0 , Integer.MAX_VALUE ) );
					}
					// load contacts
					EventTimeLine maskContact[]= new EventTimeLine[animalList.size()+1];
					for ( int id = 1 ; id < animalList.size()+1 ; id++ )
					{
						if (id == idAnimalToCheck ) continue;
						System.out.println("Loading events #" + id );
						maskContact[id] = loadTimeLine( connection, "Contact" , id, idAnimalToCheck );
						System.out.println( "Nb events loaded : " + maskContact[id].getNbEvent( 0 , Integer.MAX_VALUE ) );
					}

					System.out.println("Masking timeline loaded");

					// One BIG Image
//					IcyBufferedImage imagePolar = new IcyBufferedImage(1000, 1000, animalList.size(), DataType.FLOAT );
//					mapPolarSequence.setImage( 0 , 0 , imagePolar );

//					IcyBufferedImage imagePlanar = new IcyBufferedImage(1000, 1000, animalList.size(), DataType.FLOAT );
//					mapPlanarSequence.setImage( 0 , 0 , imagePlanar );

					int[][] angleApproach = new int[animalList.size()+1][360];

					for ( int t = minT ; t < maxT ; t+=window )
						//int t = 0;
					{
						System.out.println( (float)( t - minT ) * ( 100f / ( maxT - minT ) ) + " % --- maxT: " + maxT );

//						double nest4Density = maskNest4.getDensity( t , t + window );
//						System.out.println("Mask 4 density: " + nest4Density );
//						if( nest4Density > 0.95 )
//						{
//							System.out.println("Skip nest4");
//							continue;
//						}

						//				if( maskNest4.eventPresentAt( t , t+window ) ) { System.out.println("Skip nest4"); continue; }
						//				if( maskNest3.eventPresentAt( t , t+window ) ) { System.out.println("Skip nest3"); continue; }

						int nbAnimalDrawn = 0;
						int nbAngleAffected =0;
						//				IcyBufferedImage image = new IcyBufferedImage(1000, 1000, 4, DataType.FLOAT );
						//				mapSequence.setImage( mapSequence.getSizeT() , 0 , image );


//						TrackContainer trackContainer = DataUtil.loadAnimalData( connection, t, t+window );
						TrackContainer trackContainer = DataUtil.loadSimpleAnimalData( connection, t, t+window );

						Animal studiedAnimal = trackContainer.getAnimalWithDataBaseId( idAnimalToCheck );


						AffineTransform transform = new AffineTransform();

						for ( int tt = t ; tt < t+window; tt++ )
						{
							if( !maskStop[idAnimalToCheck].eventPresentAt( tt ) ) continue;

							MouseDetection studiedDetection = studiedAnimal.getDetectionAt(tt);
							if ( studiedDetection == null ) continue;

							transform.setToIdentity();
							transform.rotate( -studiedDetection.angle );//- Math.PI / 2d );

							for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
							{
								if ( animal == studiedAnimal ) continue;

								Event approach = maskApproach[(int) (animal.getDataBaseId())].getEventAt( tt );
								if ( approach != null )
									if( approach.getLength() > 30 ) // Approaches must be greater than 1 sec !
									{
										int nbContactAtEnd = maskContact[(int) (animal.getDataBaseId())].getNbEvent( approach.getEndFrame()-1, approach.getEndFrame()+1 );
										if( nbContactAtEnd > 0 )
										{
//											float[] imagePolarBuffer = imagePolar.getDataXYAsFloat( (int) (animal.getDataBaseId()-1) );
//											float[] imagePlanarBuffer = imagePlanar.getDataXYAsFloat( (int) (animal.getDataBaseId()-1) );

											MouseDetection detection = animal.getDetectionAt( tt );
											if ( detection != null )
											{
												// check final angle.
												{
													if ( approach.getEndFrame() == tt ) // about the final angle
													{
														// draw the studiedMouse in planar
														{
//															float[] imagePlanarBufferStudied = imagePlanar.getDataXYAsFloat( (int) (studiedAnimal.getDataBaseId()-1) );
//															for ( Point p : studiedDetection.getROI2DArea().getBooleanMask( true ).getPoints() )
//															{
//																int offsetPlanarStudied = (int) ( (int)p.getX() + (int)(p.getY()) * 1000);
//																imagePlanarBufferStudied[offsetPlanarStudied]++;
//															}

														}

														MouseDetection detectionPrevious = animal.getDetectionAt( tt );

														// Version mass center
//														{
//															// transform mass center of incoming animal
//															Point2D p = detectionPrevious.getMassCenter().toPoint2D();
//															p.setLocation( p.getX() - studiedDetection.getMassCenter().getX() , p.getY() - studiedDetection.getMassCenter().getY() );
//															Point2D transformedPoint = transform.transform( p , null );
//
//															int angleBetweenAnimals = (int) Math.toDegrees( Math.atan2( transformedPoint.getY(), transformedPoint.getX() ) );
//															if ( angleBetweenAnimals < 0 ) angleBetweenAnimals+=360;
//															angleApproach[(int)animal.getDataBaseId()][ angleBetweenAnimals ]++;
//															nbAngleAffected++;
//														}
														// Version 10 points on animal
														{
															Point2D pBack = detectionPrevious.backPoint.toPoint2D();
															Point2D pFront = detectionPrevious.frontPoint.toPoint2D();
															Point2D vector = Util.createVector( pBack, pFront );
															vector.setLocation( vector.getX() / 10d , vector.getY() / 10d );
															Point2D point = new Point2D.Double( pBack.getX(), pBack.getY() );

															// enlever ça pour retourner à l'autre version;
//															Point2D p = new Point2D.Double( pFront.getX(), pFront.getY() );

															for ( int i = 0 ; i < 10 ; i++ )
															{
																// transform mass center of incoming animal
//																Point2D p = detectionPrevious.getMassCenter().toPoint2D();
																Point2D p = new Point2D.Double( point.getX(), point.getY() );
																p.setLocation( p.getX() - studiedDetection.getMassCenter().getX() , p.getY() - studiedDetection.getMassCenter().getY() );
																Point2D transformedPoint = transform.transform( p , null );

																int angleBetweenAnimals = (int) Math.toDegrees( Math.atan2( transformedPoint.getY(), transformedPoint.getX() ) );
																if ( angleBetweenAnimals < 0 ) angleBetweenAnimals+=360;
																angleApproach[(int)animal.getDataBaseId()][ angleBetweenAnimals ]++;
																nbAngleAffected++;
																point.setLocation( point.getX() + vector.getX(), point.getY() + vector.getY() );
															}
														}

													}
												}

											}

										}
									}
							}

						}
//						System.out.println("Nb Animal drawn: " + nbAnimalDrawn );
//						System.out.println("Nb angle affected: " + nbAngleAffected );

					}

					System.out.println("Id1\tId2\tId3\tId4" );
					for ( int a = 0; a < 360; a++ )
					{
						for ( int id = 1 ; id < animalList.size()+1 ; id++)
						{
							System.out.print( angleApproach[id][a]+"\t" );
						}
						System.out.println("");
					}


					// Log image

					animalComputation.displayInSeconds();
				}
				System.out.println("Heat Map : Finished");
			}

			if ( IN_OUT_GROUP_GROUP3 )
			{
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
				}

				System.out.println( "****");
				System.out.println( "****");
				System.out.println( "****");

				int maxT = 30 * 60 * 60 * 12;
				int window = 30 * 60 * 10;

				// load contacts
				EventTimeLine contact[] = new EventTimeLine[5];
				for ( DBAnimal dbAnimalAlone : animalList )
				{
					contact[dbAnimalAlone.getId()] = loadTimeLine(connection, "Contact", dbAnimalAlone.getId(), null );
				}

				EventTimeLine detection[] = new EventTimeLine[5];
				for ( DBAnimal dbAnimalAlone : animalList )
				{
					detection[dbAnimalAlone.getId()] = loadTimeLine(connection, "Detection", dbAnimalAlone.getId(), null );
				}

				int joiner[][] = new int[5][5];
				int outer[][] = new int[5][5];

				int inOut[][] = new int[5][5];

				int lengthDistribution[] = new int[100];
				int checkWindowLength = 10;

				for ( DBAnimal dbAnimalAlone : animalList )
				{
					System.out.println( "RFID" + dbAnimalAlone.getRFID() + " / Genotype: " + dbAnimalAlone.genotype );
					EventTimeLine group3 = loadTimeLine( connection, "Group3" , dbAnimalAlone.getId() , null );

					for ( Event group3Event : group3.getBooleanEventList() )
					{
						int duration = group3Event.getLength() ; // / 10;
						if ( duration > lengthDistribution.length-1 ) duration = lengthDistribution.length-1;
						lengthDistribution[duration] ++;

						if ( group3Event.getLength() < 30 ) continue;

						boolean testOk=true;

						int idMatchingOut = 0;
						int outFrame = group3Event.getEndFrame() +1;
						{
							// check out.
							int nb=0;
							for ( DBAnimal dbAnimalCandidate : animalList )
							{
								if ( dbAnimalCandidate.getId() == dbAnimalAlone.getId() ) continue;
								{
									if ( detection[dbAnimalCandidate.getId()].eventPresentAt( outFrame ) )
										if ( !contact[dbAnimalCandidate.getId()].eventPresentAt( outFrame ) )
										{
											//									int outerId = dbAnimalCandidate.getId();
											//									outer[dbAnimalAlone.getId()][outerId]++;
											idMatchingOut = dbAnimalCandidate.getId();
											nb++;
											//									break;
										}

								}
							}

							// check constistancy on checkWindowLength
							boolean test = true;
							if ( nb == 1 )
							{
								for ( int t = outFrame ; t < outFrame+ checkWindowLength ; t++  )
								{
									if ( !detection[idMatchingOut].eventPresentAt( t ) )
									{
										test = false;
										continue;
									}

									if ( contact[idMatchingOut].eventPresentAt( t ) )
									{
										test = false;
									}
								}
							}

							if ( nb == 1 && test )
							{
								outer[dbAnimalAlone.getId()][idMatchingOut]++;
							}else
							{
								idMatchingOut =0;
								testOk = false;
							}
						}

						int idMatchingIn = 0;
						int inFrame = group3Event.getStartFrame() - 1;
						{
							// check in.
							int nb=0;
							for ( DBAnimal dbAnimalCandidate : animalList )
							{
								if ( dbAnimalCandidate.getId() == dbAnimalAlone.getId() ) continue;
								{
									if ( detection[dbAnimalCandidate.getId()].eventPresentAt( inFrame ) )
										if ( !contact[dbAnimalCandidate.getId()].eventPresentAt( inFrame ) )
										{
//											int innerId = dbAnimalCandidate.getId();
//											joiner[dbAnimalAlone.getId()][innerId]++;
											idMatchingIn = dbAnimalCandidate.getId();
											nb++;
											//break;
										}

								}
							}

							// check constistancy on checkWindowLength
							boolean test = true;
							if ( nb == 1 )
							{
								for ( int t = inFrame - checkWindowLength; t < inFrame ; t++  )
								{
									if ( !detection[idMatchingIn].eventPresentAt( t ) )
									{
										test = false;
										continue;
									}

									if ( contact[idMatchingIn].eventPresentAt( t ) )
									{
										test = false;
									}
								}
							}

							if ( nb == 1 && test )
							{
								joiner[dbAnimalAlone.getId()][idMatchingIn]++;
							}else
							{
								idMatchingIn = 0;
								testOk = false;
							}
						}

						{
							if ( testOk )
							{
								// additional post and pre test.


								for ( int t = outFrame ; t < outFrame+ checkWindowLength ; t++  )
								{
									if ( !detection[idMatchingOut].eventPresentAt( t ) )
									{
										testOk = false;
										continue;
									}

									if ( contact[idMatchingOut].eventPresentAt( t ) )
									{
										testOk = false;
									}
								}

								for ( int t = inFrame - checkWindowLength; t < inFrame ; t++  )
								{
									if ( !detection[idMatchingIn].eventPresentAt( t ) )
									{
										testOk = false;
										continue;
									}

									if ( contact[idMatchingIn].eventPresentAt( t ) )
									{
										testOk = false;
									}
								}

							}



							if ( testOk )
							{
								inOut[idMatchingIn][idMatchingOut]++;
							}
						}
					}

				}

				System.out.println( "length distribution");
				for ( int i = 0 ; i < lengthDistribution.length ; i++ )
				{
					System.out.println( lengthDistribution[i] );
				}

				System.out.println("***** in (vertical)/out (horizontal)");
				for ( int i = 1 ; i<5; i++ )
				{
					DBAnimal dbAnimal = getDBAnimalWithID( animalList,  i );
					System.out.println( i + " RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
				}
				for ( int in = 1 ; in<5; in++ )
				{
					for ( int out = 1 ; out<5; out++ )
					{
						System.out.print( inOut[in][out] + "\t");
					}
					System.out.println("");
				}


				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println("***** joiner");
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
					for ( DBAnimal dbAnimalCandidate : animalList )
					{
						System.out.println( "RFID" + dbAnimalCandidate.getRFID() + " / Genotype: " + dbAnimalCandidate.genotype + " / nbIn: " + joiner[dbAnimal.getId()][dbAnimalCandidate.getId()] );
					}
				}

				System.out.println("**** TOTAL joiner");
				for ( DBAnimal dbAnimalCandidate : animalList )
				{
					int nbInTotal = 0;
					for ( DBAnimal dbAnimal : animalList )
					{
						nbInTotal += joiner[dbAnimal.getId()][dbAnimalCandidate.getId()] ;
					}
					System.out.println( "RFID" + dbAnimalCandidate.getRFID() + " / Genotype: " + dbAnimalCandidate.genotype + " Total In: " + nbInTotal );
				}

				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println("***** outer");
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
					for ( DBAnimal dbAnimalCandidate : animalList )
					{
						System.out.println( "RFID" + dbAnimalCandidate.getRFID() + " / Genotype: " + dbAnimalCandidate.genotype + " / nbOut: " + outer[dbAnimal.getId()][dbAnimalCandidate.getId()] );
					}
				}

				System.out.println("**** TOTAL outer");
				for ( DBAnimal dbAnimalCandidate : animalList )
				{
					int nbOutTotal = 0;
					for ( DBAnimal dbAnimal : animalList )
					{
						nbOutTotal += outer[dbAnimal.getId()][dbAnimalCandidate.getId()] ;
					}
					System.out.println( "RFID" + dbAnimalCandidate.getRFID() + " / Genotype: " + dbAnimalCandidate.genotype + " Total Out: " + nbOutTotal );
				}



/*
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}
					int minT = 0;
					int maxT = DataUtil.getMaxNumberOfFrame(connection);

//								minT = (int)(30*60*60*8.25f);	// minT
//								maxT = minT+ 30*60*60;			// override

//								minT = 0;
//								maxT = 30*60*10;

					//			minT = (int)(30*60*60*14.15f);	// minT
					//			maxT = minT+ 30*60*10;			// override

					int window = 30*60*5; //  minutes

					// Load masking time lines.

					// load contacts
					EventTimeLine maskContact[][]= new EventTimeLine[animalList.size()+1][animalList.size()+1];
					int nbGetIn[][][] = new int[animalList.size()+1][animalList.size()+1][animalList.size()+1];

					for ( int idA = 1 ; idA < animalList.size()+1 ; idA++ )
					{
						for ( int idB = idA+1 ; idB < animalList.size()+1 ; idB++ )
						{
//							if ( idA == idB ) continue;

							// should be : 12 13 14 23 24 34

							System.out.println("Loading contact events idA#" + idA + " idB#" + idB );
							maskContact[idA][idB] = loadTimeLine( connection, "Contact" , idA, idB );
							maskContact[idB][idA] = maskContact[idA][idB];

							System.out.println( "Nb events loaded : " + maskContact[idA][idB].getNbEvent( 0 , Integer.MAX_VALUE ) );
						}
					}

					// load NEST4
//					EventTimeLine maskNest4= null;
//					System.out.println("Loading events #" + maskNest4 );
//					maskNest4 = loadTimeLine( connection, "Nest4" , null , null );
//					System.out.println( "Nb events loaded : " + maskNest4.getNbEvent( 0 , Integer.MAX_VALUE ) );

					for ( int t = minT ; t < maxT ; t+=window )
						//int t = 0;
					{
						System.out.println( (float)( t - minT ) * ( 100f / ( maxT - minT ) ) + " % --- maxT: " + maxT );

//						double nest4Density = maskNest4.getDensity( t , t + window );
//						System.out.println("Mask 4 density: " + nest4Density );
//						if( nest4Density > 0.95 )
//						{
//							System.out.println("Skip nest4");
//							continue;
//						}

						TrackContainer trackContainer = DataUtil.loadAnimalData( connection, t, t+window );
						//						Animal studiedAnimal = trackContainer.getAnimalWithDataBaseId( idAnimalToCheck );
						//System.out.println("Working on t : " + t + "/" + (int)(t+window) );


						for ( int tt = t ; tt < t+window; tt++ )
						{
//							if( maskNest4.eventPresentAt( tt ) ) continue;
							for ( int idA = 1 ; idA < animalList.size()+1 ; idA++ )
							{
								for ( int idB = idA+1 ; idB < animalList.size()+1 ; idB++ )
								{
									MouseDetection detectionAprev = trackContainer.getAnimalWithDataBaseId( idA ).getDetectionAt( tt-1 );
									if ( detectionAprev == null ) continue;
									MouseDetection detectionBprev = trackContainer.getAnimalWithDataBaseId( idB ).getDetectionAt( tt-1 );
									if ( detectionBprev == null ) continue;

									if ( maskContact[idA][idB].eventPresentAt( tt-1 )  ) // A and B are in contact at t-1
									{
										// check if A and B are not in contact with any others at tt-1

										boolean notInContactWithAnyOther = true;
										for ( int idToCheck = 1 ; idToCheck < animalList.size()+1 ; idToCheck++ )
										{
											if ( idToCheck != idA && idToCheck != idB )
											{
												if( maskContact[idA][idToCheck].eventPresentAt( tt-1 ) )
												{
													notInContactWithAnyOther = false;
												}
											}
										}

										if ( notInContactWithAnyOther )
										{
											MouseDetection detectionA = trackContainer.getAnimalWithDataBaseId( idA ).getDetectionAt( tt );
											if ( detectionA == null ) continue;
											MouseDetection detectionB = trackContainer.getAnimalWithDataBaseId( idB ).getDetectionAt( tt );
											if ( detectionB == null ) continue;

											for ( int idToCheck = 1 ; idToCheck < animalList.size()+1 ; idToCheck++ )
											{
												if ( idToCheck != idA && idToCheck != idB )
												{
													if( maskContact[idA][idToCheck].eventPresentAt( tt ) )
													{
														nbGetIn[idA][idB][idToCheck]++;
													}
												}

											}

										}
									}
								}
							}

						}

					}
					for ( int idA = 1 ; idA < animalList.size()+1 ; idA++ )
					{
						for ( int idB = idA+1 ; idB < animalList.size()+1 ; idB++ )
						{
							for ( int idIn = 1 ; idIn < animalList.size()+1 ; idIn++ )
							{
								if ( idIn != idA && idIn != idB )
								{
									System.out.print( idA + " / " + idB + " getting in: " + idIn + "\t" );
									System.out.println( nbGetIn[idA][idB][idIn] );
								}
							}
						}
					}
*/

			}

			if ( IN_OUT_GROUP_GROUP4 )
			{
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
				}

				System.out.println( "****");
				System.out.println( "****");
				System.out.println( "****");

				int maxT = 30 * 60 * 60 * 12;
				int window = 30 * 60 * 10;

				// load contacts
				EventTimeLine contact[] = new EventTimeLine[5];
				for ( DBAnimal dbAnimalAlone : animalList )
				{
					contact[dbAnimalAlone.getId()] = loadTimeLine(connection, "Contact", dbAnimalAlone.getId(), null );
				}

				EventTimeLine detection[] = new EventTimeLine[5];
				for ( DBAnimal dbAnimalAlone : animalList )
				{
					detection[dbAnimalAlone.getId()] = loadTimeLine(connection, "Detection", dbAnimalAlone.getId(), null );
				}

				int joiner[] = new int[5];
				int outer[] = new int[5];

				int inOut[][] = new int[5][5];

				int lengthDistribution[] = new int[100];
				int checkWindowLength = 10;

					EventTimeLine group4 = loadTimeLine( connection, "Group4" , null , null );

					for ( Event group4Event : group4.getBooleanEventList() )
					{
						int duration = group4Event.getLength() ; // / 10;
						if ( duration > lengthDistribution.length-1 ) duration = lengthDistribution.length-1;
						lengthDistribution[duration] ++;

						if ( group4Event.getLength() < 30 ) continue;

						boolean testOk=true;

						int idMatchingOut = 0;
						int outFrame = group4Event.getEndFrame() +1;
						{
							// check out.
							int nb=0;
							for ( DBAnimal dbAnimalCandidate : animalList )
							{

									if ( detection[dbAnimalCandidate.getId()].eventPresentAt( outFrame ) )
										if ( !contact[dbAnimalCandidate.getId()].eventPresentAt( outFrame ) )
										{
											//									int outerId = dbAnimalCandidate.getId();
											//									outer[dbAnimalAlone.getId()][outerId]++;
											idMatchingOut = dbAnimalCandidate.getId();
											nb++;
											//									break;
										}


							}

							// check constistancy on checkWindowLength
							boolean test = true;
							if ( nb == 1 )
							{
								for ( int t = outFrame ; t < outFrame+ checkWindowLength ; t++  )
								{
									if ( !detection[idMatchingOut].eventPresentAt( t ) )
									{
										test = false;
										continue;
									}

									if ( contact[idMatchingOut].eventPresentAt( t ) )
									{
										test = false;
									}
								}
							}

							if ( nb == 1 && test )
							{
								outer[idMatchingOut]++;
							}else
							{
								idMatchingOut =0;
								testOk = false;
							}
						}

						int idMatchingIn = 0;
						int inFrame = group4Event.getStartFrame() - 1;
						{
							// check in.
							int nb=0;
							for ( DBAnimal dbAnimalCandidate : animalList )
							{

									if ( detection[dbAnimalCandidate.getId()].eventPresentAt( inFrame ) )
										if ( !contact[dbAnimalCandidate.getId()].eventPresentAt( inFrame ) )
										{
//											int innerId = dbAnimalCandidate.getId();
//											joiner[dbAnimalAlone.getId()][innerId]++;
											idMatchingIn = dbAnimalCandidate.getId();
											nb++;
											//break;
										}


							}

							// check constistancy on checkWindowLength
							boolean test = true;
							if ( nb == 1 )
							{
								for ( int t = inFrame - checkWindowLength; t < inFrame ; t++  )
								{
									if ( !detection[idMatchingIn].eventPresentAt( t ) )
									{
										test = false;
										continue;
									}

									if ( contact[idMatchingIn].eventPresentAt( t ) )
									{
										test = false;
									}
								}
							}

							if ( nb == 1 && test )
							{
								joiner[idMatchingIn]++;
							}else
							{
								idMatchingIn = 0;
								testOk = false;
							}
						}

						{
							if ( testOk )
							{
								// additional post and pre test.


								for ( int t = outFrame ; t < outFrame+ checkWindowLength ; t++  )
								{
									if ( !detection[idMatchingOut].eventPresentAt( t ) )
									{
										testOk = false;
										continue;
									}

									if ( contact[idMatchingOut].eventPresentAt( t ) )
									{
										testOk = false;
									}
								}

								for ( int t = inFrame - checkWindowLength; t < inFrame ; t++  )
								{
									if ( !detection[idMatchingIn].eventPresentAt( t ) )
									{
										testOk = false;
										continue;
									}

									if ( contact[idMatchingIn].eventPresentAt( t ) )
									{
										testOk = false;
									}
								}

							}



							if ( testOk )
							{
								inOut[idMatchingIn][idMatchingOut]++;
							}
						}
					}



				System.out.println( "length distribution");
				for ( int i = 0 ; i < lengthDistribution.length ; i++ )
				{
					System.out.println( lengthDistribution[i] );
				}

				System.out.println("***** in (vertical)/out (horizontal)");
				for ( int i = 1 ; i<5; i++ )
				{
					DBAnimal dbAnimal = getDBAnimalWithID( animalList,  i );
					System.out.println( i + " RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
				}
				for ( int in = 1 ; in<5; in++ )
				{
					for ( int out = 1 ; out<5; out++ )
					{
						System.out.print( inOut[in][out] + "\t");
					}
					System.out.println("");
				}


				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println("***** joiner");
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
					for ( DBAnimal dbAnimalCandidate : animalList )
					{
						System.out.println( "RFID" + dbAnimalCandidate.getRFID() + " / Genotype: " + dbAnimalCandidate.genotype + " / nbIn: " + joiner[dbAnimalCandidate.getId()] );
					}
				}

				System.out.println("**** TOTAL joiner");
				for ( DBAnimal dbAnimalCandidate : animalList )
				{
					int nbInTotal = 0;

						nbInTotal += joiner[dbAnimalCandidate.getId()] ;

					System.out.println( "RFID" + dbAnimalCandidate.getRFID() + " / Genotype: " + dbAnimalCandidate.genotype + " Total In: " + nbInTotal );
				}

				for ( DBAnimal dbAnimal : animalList )
				{
					System.out.println("***** outer");
					System.out.println( "RFID" + dbAnimal.getRFID() + " / Genotype: " + dbAnimal.genotype );
					for ( DBAnimal dbAnimalCandidate : animalList )
					{
						System.out.println( "RFID" + dbAnimalCandidate.getRFID() + " / Genotype: " + dbAnimalCandidate.genotype + " / nbOut: " + outer[dbAnimalCandidate.getId()] );
					}
				}

				System.out.println("**** TOTAL outer");
				for ( DBAnimal dbAnimalCandidate : animalList )
				{
					int nbOutTotal = 0;

					{
						nbOutTotal += outer[dbAnimalCandidate.getId()] ;
					}
					System.out.println( "RFID" + dbAnimalCandidate.getRFID() + " / Genotype: " + dbAnimalCandidate.genotype + " Total Out: " + nbOutTotal );
				}


			}

			if ( TRIO_LEFT_BY_ONE )
			{
/*
				System.out.println("TO DO !!");
					int minT = 0;
					int maxT = DataUtil.getMaxNumberOfFrame(connection);

//								minT = (int)(30*60*60*8.25f);	// minT
//								maxT = minT+ 30*60*60;			// override

								minT = 0;
								maxT = 30*60*10;

					//			minT = (int)(30*60*60*14.15f);	// minT
					//			maxT = minT+ 30*60*10;			// override

					int window = 30*60*5; //  minutes

					// Load masking time lines.

					// load contacts
					EventTimeLine maskContact[][]= new EventTimeLine[5][5];
					int nbGetOut[][][][] = new int[5][5][5][5];

					for ( int idA = 1 ; idA < 5 ; idA++ )
					{
						for ( int idB = idA+1 ; idB < 5 ; idB++ )
						{

							// should be : 12 13 14 23 24 34

								System.out.println("Loading contact events idA#" + idA + " idB#" + idB );
								maskContact[idA][idB] = loadTimeLine( connection, "Contact" , idA, idB );
								maskContact[idB][idA] = maskContact[idA][idB];

								System.out.println( "Nb events loaded : " + maskContact[idA][idB].getNbEvent( 0 , Integer.MAX_VALUE ) );
						}
					}

					// load NEST4
					EventTimeLine maskNest4= null;
					System.out.println("Loading events #" + maskNest4 );
					maskNest4 = loadTimeLine( connection, "Nest4" , null , null );
					System.out.println( "Nb events loaded : " + maskNest4.getNbEvent( 0 , Integer.MAX_VALUE ) );

					for ( int t = minT ; t < maxT ; t+=window )
						//int t = 0;
					{
						System.out.println( (float)( t - minT ) * ( 100f / ( maxT - minT ) ) + " % --- maxT: " + maxT );

						double nest4Density = maskNest4.getDensity( t , t + window );
						System.out.println("Mask 4 density: " + nest4Density );
						if( nest4Density > 0.95 )
						{
							System.out.println("Skip nest4");
							continue;
						}

						TrackContainer trackContainer = DataUtil.loadAnimalData( connection, t, t+window );
						//						Animal studiedAnimal = trackContainer.getAnimalWithDataBaseId( idAnimalToCheck );
						//System.out.println("Working on t : " + t + "/" + (int)(t+window) );


						for ( int tt = t ; tt < t+window; tt++ )
						{
							if( maskNest4.eventPresentAt( tt ) ) continue;

							for ( int idA = 1 ; idA < 5 ; idA++ )
							{
								for ( int idB = idA+1 ; idB < 5 ; idB++ )
								{
									MouseDetection detectionAprev = trackContainer.getAnimalWithDataBaseId( idA ).getDetectionAt( tt-1 );
									if ( detectionAprev == null ) continue;
									MouseDetection detectionBprev = trackContainer.getAnimalWithDataBaseId( idB ).getDetectionAt( tt-1 );
									if ( detectionBprev == null ) continue;

									if ( maskContact[idA][idB].eventPresentAt( tt-1 )  ) // A and B are in contact at t-1
									{
										// check if A and B are not in contact with any others at tt-1

										boolean notInContactWithAnyOther = true;
										for ( int idToCheck = 1 ; idToCheck < 5 ; idToCheck++ )
										{
											if ( idToCheck != idA && idToCheck != idB )
											{
												if( maskContact[idA][idToCheck].eventPresentAt( tt-1 ) )
												{
													notInContactWithAnyOther = false;
												}
											}
										}

										if ( notInContactWithAnyOther )
										{
											MouseDetection detectionA = trackContainer.getAnimalWithDataBaseId( idA ).getDetectionAt( tt );
											if ( detectionA == null ) continue;
											MouseDetection detectionB = trackContainer.getAnimalWithDataBaseId( idB ).getDetectionAt( tt );
											if ( detectionB == null ) continue;

											for ( int idToCheck = 1 ; idToCheck < 5 ; idToCheck++ )
											{
												if ( idToCheck != idA && idToCheck != idB )
												{
													if( maskContact[idA][idToCheck].eventPresentAt( tt ) )
													{
														nbGetIn[idA][idB][idToCheck]++;
													}
												}

											}

										}
									}
								}
							}

						}

					}
					for ( int idA = 1 ; idA < 5 ; idA++ )
					{
						for ( int idB = idA+1 ; idB < 5 ; idB++ )
						{
							for ( int idIn = 1 ; idIn < 5 ; idIn++ )
							{
								if ( idIn != idA && idIn != idB )
								{
									System.out.print( idA + " / " + idB + " getting in: " + idIn + "\t" );
									System.out.println( nbGetIn[idA][idB][idIn] );
								}
							}
						}
					}

*/
			}

			if ( PROCESS_CONTACT_NEST )
			{
				//				DataBaseRecomputeOnlySpecitifEvents.mergeEvent( "Stop" , connection , idA, null );
				//					DataBaseRecomputeOnlySpecitifEvents.mergeEvent( "Contact" , connection , idA, idB );
				//					DataBaseRecomputeOnlySpecitifEvents.mergeEvent( "Oral-oral Contact" , connection , idA, idB );
				//					DataBaseRecomputeOnlySpecitifEvents.mergeEvent( "Oral-genital Contact" , connection , idA, idB );

				EventTimeLine timeLineNest4 = loadTimeLine( connection, "Nest4" , null , null );

				System.out.println("Timeline nest loaded : nb events : " + timeLineNest4.getNbEvent( 0 , Integer.MAX_VALUE ));

				int tab[][] = new int[5][5];
				int tabLen[][] = new int[5][5];

				for ( int idA = 1 ; idA < 5 ;idA++ )
				{
					for ( int idB = 1 ; idB < 5 ;idB++ )
					{
						if ( idA == idB ) continue;

						System.out.println("IDA: " + idA );
						System.out.println("IDB: " + idB );
						//				EventTimeLine timeLine = loadTimeLine( connection, "Contact2" , idA, idB , timeLineNest4 , false );
						EventTimeLine timeLine = loadTimeLine( connection, "Contact2" , idA, idB );
						int nbEvent = timeLine.getNbEvent( 0 , Integer.MAX_VALUE );
						System.out.println("Nb event: " + nbEvent );
						tab[idA][idB] = nbEvent;
						tab[idB][idA] = nbEvent;

						int totalLength = timeLine.getAllEventLength();
						tabLen[idA][idB] = totalLength;
						tabLen[idB][idA] = totalLength;

					}
				}

				System.out.println("NB EVENT");
				for ( int idA = 1 ; idA < 5 ;idA++ )
				{
					for ( int idB = 1 ; idB < 5 ;idB++ )
					{
						System.out.print( tab[idA][idB] );
						System.out.print( "\t" );
					}
					System.out.println("");
				}
				System.out.println("--");
				System.out.println("EVENT LEN");
				for ( int idA = 1 ; idA < 5 ;idA++ )
				{
					for ( int idB = 1 ; idB < 5 ;idB++ )
					{
						System.out.print( tabLen[idA][idB] );
						System.out.print( "\t" );
					}
					System.out.println("");
				}

			}

			try {
				connection.commit();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
		System.out.println("***********************************");
		System.out.println("** ProcessDataBase: all finished **");
		System.out.println("***********************************");
	}

	private DBAnimal getDBAnimalWithID(ArrayList<DBAnimal> animalList, int id) {

		for ( DBAnimal dbAnimal : animalList )
		{
			if ( dbAnimal.getId() == id ) return dbAnimal;
		}
		return null;
	}

	private EventTimeLine loadTimeLine(Connection connection, String eventName, Integer idAnimalA, Integer idAnimalB ) {
		return loadTimeLine(connection, eventName, idAnimalA, idAnimalB, false );
	}

	private EventTimeLine loadTimeLine(Connection connection,
			String eventName, Integer idAnimalA, Integer idAnimalB, boolean invert ) {

		System.out.println("Loading time line: " + eventName + " " + idAnimalA +" / " + idAnimalB );

		try {
			String sql = "SELECT * FROM EVENT WHERE NAME=?";
			if( idAnimalA  != null )
			{
				sql+=" AND IDANIMALA=?";
			}
			if( idAnimalB  != null )
			{
				sql+=" AND IDANIMALB=?";
			}
			PreparedStatement ps;
			ps = connection.prepareStatement( sql );

			ps.setString( 1 , eventName );

			if( idAnimalA  != null )
			{
				ps.setInt( 2 , idAnimalA );
			}
			if( idAnimalB  != null )
			{
				ps.setInt( 3 , idAnimalB );
			}

//			Chronometer chrono = new Chronometer("Base query");
			ResultSet rs = ps.executeQuery( );
			EventTimeLine timeLine = new EventTimeLine( eventName , TimeLineDataType.BOOLEAN );
//			chrono.displayInSeconds();


			// fast version (limited in size)

			Chronometer chronoTimeLine = new Chronometer("create TimeLine");
			boolean[] timeLineBoolean = new boolean[30000000];
			while ( rs.next() )
			{
				int endFrame = rs.getInt( "endFrame" );
				int startFrame = rs.getInt( "startFrame" );
				for ( int t = startFrame ; t <= endFrame ; t++ )
				{
					timeLineBoolean[t] = true;
				}
			}

			{
				int start = -1;
				for ( int i = 0 ; i < timeLineBoolean.length ; i++ )
				{
					boolean test = timeLineBoolean[i];
					if ( invert ) test =!test;

					if ( test )
					{
						if ( start == -1 ) // init start
						{
							start = i;
						}
					}
					else
					{
						if ( start != -1 )
						{
							timeLine.addEvent( new Event( timeLine.eventType, start, i ));
							start =-1;
						}
					}
				}
			}


//					boolean addEvent = true;
//
//					if ( timeLineMask!=null )
//					{
//						if( timeLineMask.eventPresentAt( t ) == eventMaskIfTrue )
//						{
//							addEvent = false;
//						}
//					}
//
//					if ( addEvent )
//					{
//						if ( CHECK_DATABASE_MODE_ON )
//						{
//							if ( timeLine.eventPresentAt( t ) )
//							{
//								System.out.println("Event ***"+ eventName + "*** already existing at t="+t );
//							}
//						}
//
//						timeLine.addPunctualEvent(t);
//					}
//				}

			chronoTimeLine.displayInSeconds();
			return timeLine;


/*
			//slow version
			Chronometer chronoTimeLine = new Chronometer("create TimeLine");
			while ( rs.next() )
			{
				int endFrame = rs.getInt( "endFrame" );
				int startFrame = rs.getInt( "startFrame" );
				for ( int t = startFrame ; t <= endFrame ; t++ )
				{
					boolean addEvent = true;

					if ( timeLineMask!=null )
					{
						if( timeLineMask.eventPresentAt( t ) == eventMaskIfTrue )
						{
							addEvent = false;
						}
					}

					if ( addEvent )
					{
						if ( CHECK_DATABASE_MODE_ON )
						{
							if ( timeLine.eventPresentAt( t ) )
							{
								System.out.println("Event ***"+ eventName + "*** already existing at t="+t );
							}
						}

						timeLine.addPunctualEvent(t);
					}
				}
			}
			chronoTimeLine.displayInSeconds();
			return timeLine;
*/

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;

	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {

		g.setColor( Color.white );

		g.drawLine( 500, 250, 500, 750 );
		g.drawLine( 250, 500, 750, 500 );

		for ( AngleMarker angleMarker : angleMarkerList )
		{
			Line2D line = new Line2D.Double( angleMarker.start , angleMarker.target );

			g.draw( line );
			g.drawString( ""+angleMarker.id+ "/" +angleMarker.angle , (int)angleMarker.target.getX() , (int)angleMarker.target.getY() );

		}

	}

	@Override
	public void mousePressed(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseMove(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseDrag(MouseEvent e, Point2D imagePoint, IcyCanvas canvas) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased(KeyEvent e, Point2D imagePoint, IcyCanvas canvas) {
		// TODO Auto-generated method stub

	}



}
