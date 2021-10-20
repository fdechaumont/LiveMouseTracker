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

import icy.canvas.IcyCanvas;
import icy.image.IcyBufferedImage;
import icy.painter.Painter;
import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import icy.sequence.Sequence;
import icy.system.profile.Chronometer;
import icy.type.DataType;
import plugins.fab.livemousetracker.Animal;
import plugins.fab.livemousetracker.dataplayer.DBAnimal;
import plugins.fab.livemousetracker.dataplayer.DataUtil;
import plugins.fab.livemousetracker.detection.MouseDetection;
import plugins.fab.livemousetracker.liveanalysis.chronogram.Event;
import plugins.fab.livemousetracker.liveanalysis.chronogram.EventTimeLine;
import plugins.fab.livemousetracker.liveanalysis.chronogram.TimeLineDataType;
import plugins.fab.livemousetracker.track.TrackContainer;
import plugins.fab.livemousetracker.track.TrackSegment;

public class ProcessRearingQuestion extends PluginActionable implements PluginThreaded, Painter {

	private static final boolean PREDECESSOR_REARING = false;

	private boolean CHECK_DATABASE_MODE_ON = false;

	Connection connection = null;

	@Override
	public void run() {

		File files[] = DataUtil.selectDataBaseFiles( this );

		for ( File dataBaseFile : files )
		{
			System.out.println("*************************************************");
			System.out.println("Processing file: " + dataBaseFile.getAbsolutePath() );
			System.out.println("*************************************************");

			connection = null;
			connection = DataUtil.connectDataBase( connection, dataBaseFile );

			System.out.println("Loading animals.");
			ArrayList<DBAnimalRearing> animalList = new ArrayList<DBAnimalRearing>() ;
			{
				ArrayList<DBAnimal> animalDBList = null ;
				try {
					animalDBList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				for ( DBAnimal animal: animalDBList )
				{
					animalList.add( new DBAnimalRearing( animal.getId() , animal.getRFID() ) );
				}
			}

			for ( DBAnimalRearing animal : animalList )
			{
				System.out.println("-------");
				System.out.println("Loading rearing events for " + animal );
				Chronometer chrono = new Chronometer( "Loading rearing infos...");
				animal.rearingTimeLine = loadTimeLine( connection, "Rearing" , animal.getId() , null );
				System.out.println( animal.rearingTimeLine );
				animal.rearingTimeLine.removeEventLessThanLength( 4 );
				System.out.println( animal.rearingTimeLine );
				chrono.displayInSeconds();

				System.out.println("*Load rich events");
				animal.enrichEvents( connection );
			}

			System.out.println("-------------");
			System.out.println("Seek for data");
			System.out.println("-------------");

			for ( DBAnimalRearing animalA : animalList )
			{
				for ( DBAnimalRearing animalB : animalList )
				{
					if ( animalA == animalB ) continue;

					System.out.println( animalA + " mimiced by " + animalB );

					animalA.seekForMimic( animalB );
				}
			}

			for ( DBAnimalRearing animal : animalList )
			{
				System.out.println( animal );
				animal.displayGlobalHisto();
			}

/*
			if ( CHECK_DATABASE )
			{
				CHECK_DATABASE_MODE_ON = true;
				EventTimeLine timeLine = loadTimeLine( connection, "NEST4" , null , null );
				CHECK_DATABASE_MODE_ON = false;
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
*/
/*
			if ( HEATMAP_BASIC ) // A etendre sur location ?
			{
				Sequence mapSequence = new Sequence("HeatMap");
				IcyBufferedImage image = new IcyBufferedImage(1000, 1000, 4, DataType.FLOAT );

				int maxT = DataUtil.getMaxNumberOfFrame(connection);
				int window = 30*60*10; // 10 minutes
				for ( int t = 0 ; t < maxT ; t+=window )

				{
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
									int offset = p.x + p.y * 1000;
									imageBuffer[offset]++;
								}
							}
						}
					}

				}

				mapSequence.addImage( image );
				addSequence( mapSequence );
				System.out.println("Heat Map : Finished");
			}
*/
/*
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
*/
/*
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
*/
/*
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
*/
/*
			// take an animal and plot the others animal by re rotating the animal and position it at the center
			if ( HEATMAP_CENTERED_ON_ANIMAL )
			{
				ArrayList<DBAnimal> animalList = null ;
				try {
					animalList = DataUtil.loadMice(connection);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				for ( int idAnimalToCheck=1 ; idAnimalToCheck< animalList.size()+1; idAnimalToCheck ++ )
				{
					//int idAnimalToCheck=1;
					System.out.println("ANIMAL TO CHECK ID = " + idAnimalToCheck );
					Sequence mapPolarSequence = new Sequence("HeatMap Polar " + dataBaseFile.getName() + " id " + idAnimalToCheck );
					//				mapPolarSequence.addPainter( this );
					addSequence( mapPolarSequence );

//					Sequence mapPlanarSequence = new Sequence("HeatMap Planar id " + idAnimalToCheck );
//					addSequence( mapPlanarSequence );


					int minT = 0;
					int maxT = DataUtil.getMaxNumberOfFrame(connection);

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
					// load stops
					EventTimeLine maskStop[]= new EventTimeLine[animalList.size()+1];
					for ( int id = 1 ; id < animalList.size()+1 ; id++ )
					{
//						if (id == idAnimalToCheck ) continue;
						System.out.println("Loading events #" + id );
						maskStop[id] = loadTimeLine( connection, "Stop" , idAnimalToCheck, null );
						System.out.println( "Nb events loaded : " + maskStop[id].getNbEvent( 0 , Integer.MAX_VALUE ) );
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

					System.out.println("Masking timeline loaded");

					// One BIG Image
					IcyBufferedImage imagePolar = new IcyBufferedImage(1000, 1000, animalList.size(), DataType.FLOAT );
					mapPolarSequence.setImage( 0 , 0 , imagePolar );

//					IcyBufferedImage imagePlanar = new IcyBufferedImage(1000, 1000, animalList.size(), DataType.FLOAT );
//					mapPlanarSequence.setImage( 0 , 0 , imagePlanar );

					int[][] angleApproach = new int[animalList.size()+1][36];

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
						//				if( maskNest4.eventPresentAt( t , t+window ) ) { System.out.println("Skip nest4"); continue; }
						//				if( maskNest3.eventPresentAt( t , t+window ) ) { System.out.println("Skip nest3"); continue; }

						int nbAnimalDrawn = 0;
						//				IcyBufferedImage image = new IcyBufferedImage(1000, 1000, 4, DataType.FLOAT );
						//				mapSequence.setImage( mapSequence.getSizeT() , 0 , image );
						TrackContainer trackContainer = DataUtil.loadAnimalData( connection, t, t+window );
						Animal studiedAnimal = trackContainer.getAnimalWithDataBaseId( idAnimalToCheck );
						System.out.println("Working on t : " + t + "/" + (int)(t+window) );

						for ( int tt = t ; tt < t+window; tt++ )
						{
							if( maskNest4.eventPresentAt( tt ) ) continue;
							if( maskNest3.eventPresentAt( tt ) ) continue;
							if( !maskStop[idAnimalToCheck].eventPresentAt( tt ) ) continue;

							MouseDetection studiedDetection = studiedAnimal.getDetectionAt(tt);
							if ( studiedDetection == null ) continue;

							AffineTransform transform = new AffineTransform();
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
											float[] imagePolarBuffer = imagePolar.getDataXYAsFloat( (int) (animal.getDataBaseId()-1) );
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
														angleApproach[(int)animal.getDataBaseId()][ angleBetweenAnimals/10 ]++;

														transformedPoint.setLocation( transformedPoint.getX()+500, transformedPoint.getY() + 500 );
														angleMarkerList.add( new AngleMarker( new Point2D.Double( 500 , 500 ),
																transformedPoint ,angleBetweenAnimals , (int)animal.getDataBaseId() ) );

													}
												}

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
															int offsetPolar = (int) ( (int)transformedPoint.getX() + (int)(transformedPoint.getY()) * 1000);
															imagePolarBuffer[offsetPolar]++;
														}
													}
												}
											}

										}
									}
							}

						}
						imagePolar.dataChanged();
//						imagePlanar.dataChanged();

						// name channels
						{
							for ( Animal animal : trackContainer.animalTrackSegmentPool.animalList )
							{
								mapPolarSequence.setChannelName( (int)(animal.getDataBaseId()-1) , animal.getRfidID().substring( animal.getRfidID().length()-4 ) );
							}
						}
						System.out.println("Nb Animal drawn: " + nbAnimalDrawn );

					}

					System.out.println("Id1\tId2\tId3\tId4" );
					for ( int a = 0; a < 36; a++ )
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
						for ( int t = 0 ; t < mapPolarSequence.getSizeT() ; t++ )
						{
							IcyBufferedImage imageToLog = mapPolarSequence.getImage( t , 0 );
							for ( int c = 0 ; c<imageToLog.getSizeC() ; c++ )
							{
								float [] data = imageToLog.getDataXYAsFloat( c );
								for ( int i = 0 ; i< data.length ; i++ )
								{
									float value = (float)Math.log10( data[i] );
									if ( value < 0 ) value = 0;
									data[i] = value;
								}
							}
							imageToLog.dataChanged();
						}
					}
				}
				System.out.println("Heat Map : Finished");
			}
*/

			/*
			if ( DUO_JOINED_BY_THIRD )
			{

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


			}
*/

/*
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
*/
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

			System.out.println("***********************************");
			System.out.println("** ProcessDataBase: all finished **");
			System.out.println("***********************************");
		}
	}

	private EventTimeLine loadTimeLine(Connection connection, String eventName, Integer idAnimalA, Integer idAnimalB ) {
		return loadTimeLine(connection, eventName, idAnimalA, idAnimalB, null, false );
	}

	private EventTimeLine loadTimeLine(Connection connection,
			String eventName, Integer idAnimalA, Integer idAnimalB, EventTimeLine timeLineMask , boolean eventMaskIfTrue ) {

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
//			sql += " AND STARTFRAME<36000"; // 20 first minutes

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

			ResultSet rs = ps.executeQuery( );
			EventTimeLine timeLine = new EventTimeLine( eventName , TimeLineDataType.BOOLEAN );

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
			return timeLine;

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

//		for ( AngleMarker angleMarker : angleMarkerList )
//		{
//			Line2D line = new Line2D.Double( angleMarker.start , angleMarker.target );
//
//			g.draw( line );
//			g.drawString( ""+angleMarker.id+ "/" +angleMarker.angle , (int)angleMarker.target.getX() , (int)angleMarker.target.getY() );
//
//		}

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
