package petriwars;

import java.util.ArrayList;

import petriwars.types.Map;
import petriwars.types.Point;
import petriwars.ServerManager;

public class WaypointManager {
	public ArrayList<Path> path_list;
	public WaypointManager(){
		path_list = new ArrayList<Path>();
	}

	public static class Path{
		ArrayList<WP> waypt_list;
		Map map;
		public Path(double d, double e, double f, double g, Map m){
			
			map = m;
			waypt_list = new ArrayList<WP>();
			waypt_list.add(new WP(new Point(d, e), new Point(f, g), null));
			waypt_list.add(new WP(new Point(f, g), new Point(f, g), waypt_list.get(0)));
			expand_path();
		}
		
		public class WP{
			Point loc;
			WP prev;
			double dist_left, dist_travelled;
			public WP(Point loc, Point dest, WP prev){
				this.loc = loc;
				this.prev = prev;
				dist_left = (Math.abs(dest.x-loc.x)) + (Math.abs(dest.y-loc.y));
				if(prev==null) dist_travelled =0;
				else dist_travelled = prev.dist_travelled + Math.abs(prev.loc.x-loc.x) + Math.abs(prev.loc.y-loc.y);
			}
		}
		
		public void expand_path(){//add all of the way-points to the list
			//necessary bool :(
			boolean path_found=false;
			Point dest = waypt_list.get(waypt_list.size()-1).loc;
			WP cur = waypt_list.get(0);
			ArrayList<WP> path_tree = new ArrayList<WP>();
			path_tree.add((WP) waypt_list.get(0));
			
			//figure out which squares the unit path hits
			ArrayList<Point> intersects;
			ArrayList<Point> corners = new ArrayList<Point>();
			//corners.sort(); //and also shave
			while(path_found==false){
				intersects = get_square_intersects(waypt_list.get(0).loc, dest);
				if(intersects.isEmpty()==false)System.out.println("Intersect: " + intersects.get(0).x + ", " + intersects.get(0).y);
				if(intersects.isEmpty()){path_found=true; break;}//done searching for waypoints
				for(int i=0; i<intersects.size(); i++){
					if(map.getObstacleAt((int)intersects.get(i).x, (int)intersects.get(i).y)!=null){
						corners = map.getObstacleAt((int)intersects.get(i).x, (int)intersects.get(i).y).getCorners();
						corners = pick_corners(corners, cur.loc);
						break;
					}
				}
				if(corners.isEmpty()) continue;
				print_intersects(intersects);
				//dont remove so that u can stil ref previous ones
				cur = path_tree.get(next_path_pick(path_tree));
				for(int i=0; i<corners.size(); i++){
					path_tree.add(new WP(corners.get(i), dest, cur));
				}
			}
			//reassemble the path
			while(cur!=null){
				waypt_list.add(1, cur);
				cur = cur.prev;
			}
		}
		
		public ArrayList<Point> get_square_intersects(Point s, Point e){//TODO fix this to use diff start AND end locations (also used by corner sorter)
			ArrayList<Point> intersects = new ArrayList<Point>();
			WP start = new WP(s, e, null);
			WP end = new WP(e, e, null);
			double slope;//slope of the line
			int left_right;//if ray is moving left or right (-1, or 1)
			Point end_square;//square map coor of path destination
			double xpos;//x pos on map
			double ypos;//y pos on map
			//get important variables
			slope = (end.loc.y - start.loc.y) / (end.loc.x - start.loc.x);
			left_right = (int) Math.signum(end.loc.x - start.loc.x);
			end_square = new Point((int)end.loc.x, (int)end.loc.y);
			//find each square on map that path hits
			ypos=start.loc.y;
			xpos=start.loc.x;
			if(left_right>0){//check if you are moving right
				if(slope==1){
					xpos-=1; ypos-=1;
					while((int)xpos<(int)end_square.x && (int)ypos<(int)end_square.y){
						xpos+=1;ypos+=1;
						intersects.add(new Point((int)(xpos), (int) ypos));//add to intersects
					}
				}
				if(slope==-1){
					xpos-=1; ypos+=1;
					while((int)xpos<(int)end_square.x && (int)ypos>(int)end_square.y){
						xpos+=1;ypos-=1;
						intersects.add(new Point((int)(xpos), (int) ypos));//add to intersects
					}
				}
				
				
				if(slope>1){
					xpos-=1; ypos-=slope;
					while((int)xpos<(int)end_square.x && (int)ypos<(int)end_square.y){//until you reach the destination square
						//increment xpos and ypos
						xpos+=1; ypos+=slope;
						for(int i=(int)ypos; i<=(int)(ypos+slope); i++){//get all int values of y incurred in this int x
							intersects.add(new Point((int)(xpos), i));//add to intersects
							if((int)xpos==(int)end_square.x && i==(int)end_square.y)break;
						}
					}
				}
				else if (slope<1 && slope>0){
					slope=(float) Math.pow(slope, -1);//get slope inverse
					ypos-=1; xpos-=slope;
					while((int)xpos<(int)end_square.x && (int)ypos<(int)end_square.y){//until you reach the destination square
						//increment xpos and ypos
						ypos+=1; xpos+=slope;
						for(int i=(int)xpos; i<=(int)(xpos+slope); i++){//get all int values of y incurred in this int x
							intersects.add(new Point(i, (int)ypos));//add to intersects
							if(i==(int)end_square.x && (int)ypos==(int)end_square.y)break;
						}
						System.out.println("rounding? :" + xpos + " " + ypos);
					}
				}
				else if (slope==0){
					while((int)xpos!=(int)end_square.x || (int)ypos!=(int)end_square.y){//until you reach the destination square
						xpos++;
						intersects.add(new Point((int)xpos, (int)ypos));
					}
				}
				else if (slope<0 && slope>-1){
					slope=(float) Math.pow(slope, -1);//get slope inverse
					ypos+=1; xpos+=slope;
					while((int)xpos<(int)end_square.x && (int)ypos>(int)end_square.y){//until you reach the destination square
						//increment xpos and ypos
						ypos-=1; xpos-=slope;
						for(int i=(int)xpos; i<=(int)(xpos-slope); i++){//get all int values of y incurred in this int x
							intersects.add(new Point(i, (int)ypos));//add to intersects
							if(i==(int)end_square.x && (int)ypos==(int)end_square.y)break;
						}
						System.out.println("rounding? :" + xpos + " " + ypos);
					}
				}
				else if (slope<-1){
					xpos-=1; ypos-=slope;
					while((int)xpos<(int)end_square.x && (int)ypos>(int)end_square.y){//until you reach the destination square
						//increment xpos and ypos
						xpos+=1; ypos+=slope;
						for(int i=(int)ypos; i>=(int)(ypos+slope); i--){//get all int values of y incurred in this int x
							intersects.add(new Point((int)(xpos), i));//add to intersects
							if((int)xpos==(int)end_square.x && i==(int)end_square.y)break;
						}
						System.out.println("rounding? :" + xpos + " " + ypos);
					}
				}
			}
			else if (left_right<0){//you may be moving left
				if(slope>=1){
					while((int)xpos>(int)end_square.x && (int)ypos<(int)end_square.y){//until you reach the destination square
						for(int i=(int)ypos; i<=(int)(ypos+slope); i++){//get all int values of y incurred in this int x
							intersects.add(new Point((int)(xpos), i));//add to intersects
						}
						//increment xpos and ypos
						xpos-=1; ypos+=slope;
					}
				}
				else if (slope<1 && slope>0){
					slope=(float) Math.pow(slope, -1);//get slope inverse
					while((int)xpos>(int)end_square.x && (int)ypos<(int)end_square.y){//until you reach the destination square
						for(int i=(int)xpos; i>=(int)(xpos+slope); i--){//get all int values of y incurred in this int x
							intersects.add(new Point(i, (int)ypos));//add to intersects
						}
						//increment xpos and ypos
						ypos+=1; xpos-=slope;
					}
				}
				else if (slope==0){
					while((int)xpos!=(int)end_square.x || (int)ypos!=(int)end_square.y){//until you reach the destination square
						xpos--;
						intersects.add(new Point((int)xpos, (int)ypos));
					}
				}
				else if (slope<0 && slope>-1){
					slope=(float) Math.pow(slope, -1);//get slope inverse
					while((int)xpos>(int)end_square.x && (int)ypos>(int)end_square.y){//until you reach the destination square
						for(int i=(int)xpos; i>=(int)(xpos+slope); i--){//get all int values of y incurred in this int x
							intersects.add(new Point(i, (int)ypos));//add to intersects
						}
						//increment xpos and ypos
						ypos-=1; xpos-=slope;
					}
				}
				else if (slope<-1){
					while((int)xpos>(int)end_square.x && (int)ypos>(int)end_square.y){//until you reach the destination square
						for(int i=(int)ypos; i>=(int)(ypos+slope); i--){//get all int values of y incurred in this int x
							intersects.add(new Point((int)(xpos), i));//add to intersects
						}
						//increment xpos and ypos
						xpos-=1; ypos-=slope;
					}
				}
			}
			else{//you're moving up/down
				if(end.loc.y-ypos>0){//moving up
					while((int)xpos!=(int)end_square.x || (int)ypos!=(int)end_square.y){//until you reach the destination square
						ypos++;
						intersects.add(new Point((int)xpos, (int)ypos));
					}
				}
				else{//moving down
					while((int)xpos!=(int)end_square.x || (int)ypos!=(int)end_square.y){//until you reach the destination square
						ypos--;
						intersects.add(new Point((int)xpos, (int)ypos));
					}
				}
			}
			
			return intersects;
		}
		
		public void print_intersects(ArrayList<Point> intersects){
			System.out.println("Line from point: ( " + this.waypt_list.get(0).loc.x + ", " + this.waypt_list.get(0).loc.y + 
					") to point: (" + this.waypt_list.get(this.waypt_list.size()-1).loc.x + ", " + this.waypt_list.get(this.waypt_list.size()-1).loc.y + ")...");
			for(int i=0; i<intersects.size(); i++){
				System.out.println("(" + intersects.get(i).x + ", " + intersects.get(i).y + ")");
			}
		}
		
		public int next_path_pick (ArrayList<WP> list){
			double best_dist = list.get(0).dist_left + list.get(0).dist_travelled;;
			double cur_dist;
			int best_index = 0;
			for(int i=0; i<list.size(); i++){
				cur_dist = list.get(i).dist_left + list.get(i).dist_travelled;
				if(cur_dist < best_dist){best_dist = cur_dist; best_index = i;}
			}
			return best_index;
		}
		
		public ArrayList<Point> pick_corners (ArrayList<Point> corners, Point start){
			ArrayList<Point> ints;
			ArrayList<Point> picks = new ArrayList<Point>();
			for(int i=0; i<corners.size(); i++){
				ints=get_square_intersects(start, corners.get(i));
				if(ints.isEmpty()) picks.add(corners.get(i));
			}
			return picks;
		}
	}
	
}
