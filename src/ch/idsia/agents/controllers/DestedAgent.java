package ch.idsia.agents.controllers;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import com.sun.org.apache.bcel.internal.generic.LLOAD;

import ch.idsia.agents.Agent;
import ch.idsia.agents.controllers.DestedAgent.MarioState;
import ch.idsia.agents.controllers.DestedAgent.MoveState;
import ch.idsia.benchmark.mario.engine.Art;
import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.engine.LevelRenderer;
import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.engine.level.Level;
import ch.idsia.benchmark.mario.engine.level.SpriteTemplate;
import ch.idsia.benchmark.mario.engine.sprites.Fireball;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sparkle;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.Environment;

public class DestedAgent extends BasicMarioAIAgent implements Agent {

	public DestedAgent() {
		super("DestedAgent");
		reset();
	}

	public void reset() {
		action = new boolean[Environment.numberOfKeys];

		nextMoves = new ArrayList<boolean[]>();

	}

	public boolean[] getAction() {
		// action = new boolean[Environment.numberOfKeys];
		// this Agent requires observation integrated in advance.

		return make();

	}

	List<boolean[]> nextMoves;

	private boolean[] make() {

		if (nextMoves.size() > 0) {
			boolean[] rb = nextMoves.get(0);
			nextMoves.remove(0);
			return rb;
		}

		MarioState ms = new MarioState(Mario.instance);

		MarioState curState = getit(ms, (int) (ms.x + 350));

		if (curState != null) {
			if (curState.storedKeys.size() == 0) {
				nextMoves = new ArrayList<boolean[]>();
				nextMoves.add(curState.keys);
			} else
				nextMoves = new ArrayList<boolean[]>(curState.storedKeys);

			System.out.println("Score: " + curState.score + " Keys "
					+ keysTS(curState.keys) + " States:"
					+ curState.allStates.toString());
			System.out.print(" ");

		}

		// generateKeys();

		return make();

	}

	MarioState getit(MarioState current, int goalX) {

		PriorityQueue<MarioState> open = new PriorityQueue<MarioState>();
		ArrayList<MarioState> closed = new ArrayList<MarioState>();

		ArrayList<MarioState> visitados = new ArrayList<MarioState>();
		open.add(current);

		int iteration = 0;

		while (!open.isEmpty()) {
			current = open.poll();
			if (visitados.size() > 4500) {

				break;
			}

			visitados.add(current);

			if (isGoal(current, goalX))
				break;
			else {
				ArrayList<MarioState> irmaos = getNeighbors(current, iteration,
						goalX);
				closed.add(current);

				current.iteration = iteration++;
				for (MarioState noh : irmaos) {
					boolean bopen = false;
					for (MarioState p : visitados) {
						if (p.equals(noh))
							bopen = true;
					}
					if (!bopen) {
						open.add(noh);
					}
				}
			}
		}

		LevelRenderer.maxIter = closed.size();
		LevelRenderer.redrawStates = visitados;

		return current;
	}

	private boolean isGoal(MarioState current, int goal) {

		return (current.x > goal - 4 && current.x < goal + 4);
	}

	int[][] keyz = new int[][] { new int[] { Mario.KEY_LEFT, Mario.KEY_SPEED },
			new int[] { Mario.KEY_RIGHT, Mario.KEY_SPEED },
			new int[] { Mario.KEY_JUMP, Mario.KEY_LEFT },
			new int[] { Mario.KEY_JUMP, Mario.KEY_RIGHT },
			new int[] { Mario.KEY_JUMP, Mario.KEY_LEFT, Mario.KEY_SPEED },
			new int[] { Mario.KEY_JUMP, Mario.KEY_RIGHT, Mario.KEY_SPEED },
			new int[] { Mario.KEY_LEFT }, new int[] { Mario.KEY_RIGHT } };

	public boolean canJumpHigher(MarioState currentPos) {
		return currentPos.mayJump || (currentPos.jumpTime > 0);
	}

	private ArrayList<MarioState> getNeighbors(MarioState current,
			int iteration, float goalX) {

		ArrayList<MarioState> ms = new ArrayList<DestedAgent.MarioState>();

		for (int i = 0; i < keyz.length; i++) {

			if (keyz[i][0] == Mario.KEY_JUMP && !canJumpHigher(current))
				continue;

			MarioState m = new MarioState(current);
			// maybe =false
			for (int js : keyz[i]) {
				m.keys[js] = true;
			}
			if (iteration < 2)
				m.storeKeys();
			int nScore = 0;
			List<MoveState> fs = new ArrayList<MoveState>();
			fs.addAll(m.move());

			m.states = fs;
			for (MoveState moveState : fs) {
				switch (moveState) {
				case Collided:
					nScore += 120;
					break;
				case CriticalFail:
					continue;
					// nScore += limit * 2;
					// break;
				case Won:
					nScore = -54654654;
					break;
				case Jumping:
					nScore -= 60;
					break;
				case JumpingWasSliding:
					nScore -= 160;
					break;
				case MovingAway:
					nScore -= 80;
					break;
				case MovingTowards:
					nScore -= 100;
					break;
				case NotRunning:
					nScore += 150;
					break;
				case TooClose:
					nScore += 350;
					break;
				case Running:
					nScore -= 61;
					break;
				case NothingBelow:
					nScore += 40;
					break;
				case Sliding:
					nScore -= 200;
					break;
				case Stopped:
					nScore += 300;
					break;
				}
			}

			nScore -= (m.x - current.x) * 6;
			nScore -= ((15 * 16) - m.y) / 5.5;
			// nScore = (int) (goalX - m.x);

			// nScore -= ((15*16)-m.y) * 5;
			// if (nScore < 30)
			ms.add(m);

			// nScore+=(j*30);
			m.score = nScore;
		}
		return ms;
	}

	private void generateKeys() {

		MarioState original = new MarioState(Mario.instance);

		int[][] keyz = new int[][] {
				new int[] { Mario.KEY_LEFT, Mario.KEY_SPEED },
				new int[] { Mario.KEY_RIGHT, Mario.KEY_SPEED },
				new int[] { Mario.KEY_LEFT, Mario.KEY_JUMP },
				new int[] { Mario.KEY_RIGHT, Mario.KEY_JUMP },
				new int[] { Mario.KEY_LEFT, Mario.KEY_SPEED, Mario.KEY_JUMP },
				new int[] { Mario.KEY_RIGHT, Mario.KEY_SPEED, Mario.KEY_JUMP },
				new int[] { Mario.KEY_LEFT }, new int[] { Mario.KEY_RIGHT } };

		List<MarioState> mz = new ArrayList<MarioState>();
		mz.add(original);
		int limit = 1000;
		for (int j = 0; j < 5; j++) {
			List<MarioState> mzc = new ArrayList<MarioState>();

			Collections.sort(mz, new Comparator<MarioState>() {
				@Override
				public int compare(MarioState o1, MarioState o2) {
					return o1.score.compareTo(o2.score);
				}
			});

			List<MarioState> mdz = new ArrayList<MarioState>();
			int lmz = mz.size();
			for (int i = 0; i < (mz.size() < lmz ? mz.size() : lmz); i++) {
				mdz.add(mz.get(i));
			}
			mz = mdz;

			for (MarioState marioState : mz) {
				for (int i = 0; i < keyz.length; i++) {
					MarioState m = new MarioState(marioState);
					// maybe =false
					for (int js : keyz[i]) {
						m.keys[js] = true;
					}
					if (j < 1)
						m.storeKeys();
					int nScore = 0;

					List<MoveState> fs = new ArrayList<MoveState>();
					for (int jc = 0; jc < 1; jc++)
						fs.addAll(doit(m));

					m.states = fs;
					for (MoveState moveState : fs) {
						switch (moveState) {
						case Collided:
							nScore += 120;
							break;
						case CriticalFail:
							nScore += limit * 2;
							break;
						case Jumping:
							nScore -= 160;
							break;
						case JumpingWasSliding:
							nScore -= 120;
							break;
						case MovingAway:
							nScore -= 80;
							break;
						case MovingTowards:
							nScore -= 100;
							break;
						case NotRunning:
							nScore += 150;
							break;
						case Running:
							nScore -= 150;
							break;
						case NothingBelow:
							nScore -= limit * 2;
						case Sliding:
							nScore -= 100;
							break;
						case Stopped:
							nScore += 300;
							break;
						}
					}

					nScore += (m.x - original.x) * 20;

					// nScore -= ((15*16)-m.y) * 5;

					// nScore+=(j*30);
					m.score = nScore;
					mzc.add(m);
				}
			}
			if (mzc.size() == 0) {
				break;
			}
			mz = mzc;

		}

		int curLowest = Integer.MAX_VALUE;
		MarioState curState = null;
		for (MarioState marioState : mz) {
			if (marioState.score < curLowest) {
				curLowest = marioState.score;
				curState = marioState;
			}
		}

		if (curState != null) {
			if (curState.storedKeys.size() == 0) {
				nextMoves = new ArrayList<boolean[]>();
				nextMoves.add(curState.keys);
			} else
				nextMoves = new ArrayList<boolean[]>(curState.storedKeys);

			System.out.println("Score: " + curState.score + " Keys "
					+ keysTS(curState.keys) + " States:"
					+ curState.allStates.toString());
			System.out.print(" ");

		}
	}

	private String keysTS(boolean[] keys) {

		String k = "";
		for (int i = 0; i < keys.length; i++) {
			if (keys[i]) {
				switch (i) {
				case Mario.KEY_DOWN:
					k += "Down ";
					break;
				case Mario.KEY_JUMP:
					k += "Jump ";
					break;
				case Mario.KEY_UP:
					k += "Up ";
					break;
				case Mario.KEY_RIGHT:
					k += "Right ";
					break;
				case Mario.KEY_LEFT:
					k += "Left ";
					break;
				case Mario.KEY_SPEED:
					k += "Speed ";
					break;
				}
			}
		}
		return k;
	}

	public enum MoveState {
		CriticalFail, NothingBelow, Running, NotRunning, Jumping, MovingAway, MovingTowards, JumpingWasSliding, Stopped, Sliding, Collided, NewIteration, TooClose, Won

	}

	public List<MoveState> doit(MarioState ms) {
		List<MoveState> mc = new ArrayList<MoveState>();

		if (ms.isWorseBlocking(ms.x, ms.y)) {
			mc.add(MoveState.CriticalFail);
			return mc;
		}

		float sideWaysSpeed = ms.keys[Mario.KEY_SPEED] ? 1.2f : 0.6f;

		if (!ms.keys[Mario.KEY_SPEED])
			mc.add(MoveState.NotRunning);

		if (ms.xa > 2) {
			ms.facing = 1;
			mc.add(MoveState.MovingTowards);
		}
		if (ms.xa < -2) {
			ms.facing = -1;
			mc.add(MoveState.MovingAway);
		}

		if (ms.keys[Mario.KEY_JUMP]
				|| (ms.jumpTime < 0 && !ms.onGround && !ms.sliding)) {
			if (ms.jumpTime < 0) {
				ms.xa = ms.xJumpSpeed;
				ms.ya = -ms.jumpTime * ms.yJumpSpeed;
				ms.jumpTime++;
				// mc.add(MoveState.Jumping);
			} else if (ms.onGround && ms.mayJump) {
				ms.xJumpSpeed = 0;
				ms.yJumpSpeed = -1.9f;
				ms.jumpTime = (int) ms.jT;
				ms.ya = ms.jumpTime * ms.yJumpSpeed;
				ms.onGround = false;
				ms.sliding = false;
				mc.add(MoveState.Jumping);
			} else if (ms.sliding && ms.mayJump) {
				ms.xJumpSpeed = -ms.facing * 6.0f;
				ms.yJumpSpeed = -2.0f;
				ms.jumpTime = -6;
				ms.xa = ms.xJumpSpeed;
				ms.ya = -ms.jumpTime * ms.yJumpSpeed;
				ms.onGround = false;
				ms.sliding = false;
				ms.facing = -ms.facing;
				mc.add(MoveState.JumpingWasSliding);

			} else if (ms.jumpTime > 0) {
				ms.xa += ms.xJumpSpeed;
				ms.ya = ms.jumpTime * ms.yJumpSpeed;
				ms.jumpTime--;
				mc.add(MoveState.Jumping);
			}
		} else {
			ms.jumpTime = 0;
		}

		if (ms.keys[Mario.KEY_LEFT] && !ms.ducking) {
			if (ms.facing == 1)
				ms.sliding = false;
			ms.xa -= sideWaysSpeed;
			if (ms.jumpTime >= 0)
				ms.facing = -1;
		}

		if (ms.keys[Mario.KEY_RIGHT] && !ms.ducking) {
			if (ms.facing == -1)
				ms.sliding = false;
			ms.xa += sideWaysSpeed;
			if (ms.jumpTime >= 0)
				ms.facing = 1;
		}

		if ((!ms.keys[Mario.KEY_LEFT] && !ms.keys[Mario.KEY_RIGHT])
				|| ms.ducking || ms.ya < 0 || ms.onGround) {
			ms.sliding = false;
		}

		ms.ableToShoot = !ms.keys[Mario.KEY_SPEED];

		ms.mayJump = (ms.onGround || ms.sliding) && !ms.keys[Mario.KEY_JUMP];

		ms.xFlipPic = ms.facing == -1;

		ms.runTime += (Math.abs(ms.xa)) + 5;
		if (Math.abs(ms.xa) < 0.5f) {
			ms.runTime = 0;
			ms.xa = 0;
		}

		float oldx = ms.x;
		float oldy = ms.y;

		float running = 4;
		ms.onGround = false;

		boolean didntCollide = ms.move(ms.xa, 0);
		didntCollide = ms.move(0, ms.ya) ? true : didntCollide;
		if (Math.abs(ms.x - oldx) > running) {
			mc.add(MoveState.Running);
		}

		if (ms.isBlocking(ms.x + 1, ms.y, 0, 0)) {
			mc.add(MoveState.Collided);
		}

		if (Math.floor(ms.x) > Math.floor(oldx))
			mc.add(MoveState.MovingTowards);
		else if (Math.floor(ms.x) < Math.floor(oldx))
			mc.add(MoveState.MovingAway);
		else if (Math.floor(ms.x) == Math.floor(oldx))
			if (!ms.sliding)
				mc.add(MoveState.Stopped);

		boolean bad = true;
		for (int jc = (int) (ms.y / LevelScene.cellSize); jc < ms.levelScene.level.height + 1; jc++) {
			if (!ms.levelScene.level.isBlocking((int) (ms.x / 16), jc,
					(float) 0, (float) 0)) {
				bad = false;
				break;
			}
		}
		if (bad)
			mc.add(MoveState.NothingBelow);

		if (ms.y > ms.levelScene.level.height * LevelScene.cellSize
				+ LevelScene.cellSize)
			mc.add(MoveState.CriticalFail);
		if (ms.x > ms.levelScene.getLevelLength() * 16)
			mc.add(MoveState.Won);
		if (!didntCollide && !ms.sliding) {
			mc.add(MoveState.Collided);
		}
		if (ms.sliding)
			mc.add(MoveState.Sliding);

		ms.ya *= 0.85f;

		// if /

		if (!ms.onGround) {
			// ya += 3;

			ms.ya += ms.yaa;
		}

		return mc;
	}

	public class MarioState implements Comparable<MarioState> {
		public float runTime;
		public boolean xFlipPic;
		public int iteration;
		public int lastScore;
		public List<MoveState> states;
		public List<MoveState> allStates;
		public List<boolean[]> storedKeys;
		public Integer score = 0;
		public float jT;
		public boolean mayJump;
		public float xJumpSpeed;
		public float yJumpSpeed;
		public boolean ducking;
		public int jumpTime;
		boolean canJump;
		public Float x;
		public float y;
		float xa;
		float ya;
		boolean[] keys;
		int facing;
		boolean ableToShoot;
		boolean onGround;
		boolean sliding;
		LevelScene levelScene;
		int width = 4;
		int height = 24;
		private float yCam;
		private float xCam;
		public float yaa;

		public MarioState parent;

		public boolean large;

		public MarioState() {
			keys = new boolean[7];
		}

		public void storeKeys() {
			storedKeys.add(keys);

		}

		public int xPic, yPic;
		public int wPic = 32;
		public int hPic = 32;
		public int xPicO, yPicO;
		public Image[][] sheet;
		boolean fire = false;

		public void draw(Graphics2D og) {

			if (large) {
				sheet = Art.mario;
				if (fire)
					sheet = Art.fireMario;

				xPicO = 16;
				yPicO = 31;
				wPic = hPic = 32;
			} else {
				sheet = Art.smallMario;

				xPicO = 8;
				yPicO = 15;
				wPic = hPic = 16;
			}

			calcPic();

			// int xPixel = (int)(xOld+(x-xOld)*cameraOffSet)-xPicO;
			// int yPixel = (int)(yOld+(y-yOld)*cameraOffSet)-yPicO;

			int xPixel = (int) (x - xPicO);
			int yPixel = (int) (y - yPicO);

			// System.out.print("xPic = " + xPic);
			// System.out.print(", yPic = " + yPic);
			// System.out.println(", kind = " + this.kind);

			try {
				og.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, .25f));

				og.drawImage(sheet[xPic][yPic], xPixel + (xFlipPic ? wPic : 0)
						- (int) Mario.instance.levelScene.xCam, yPixel
						+ (false ? hPic : 0), xFlipPic ? -wPic : wPic,
						false ? -hPic : hPic, null);
				og.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 1f));

			} catch (ArrayIndexOutOfBoundsException ex) {
				// System.err.println("ok:" + this.kind + ", " + xPic);
			}
		}

		private void calcPic() {
			int runFrame;

			if (large) {
				runFrame = ((int) (runTime / 20)) % 4;
				if (runFrame == 3)
					runFrame = 1;
				if (Math.abs(xa) > 10)
					runFrame += 3;
				if (!onGround) {
					if (Math.abs(xa) > 10)
						runFrame = 7;
					else
						runFrame = 6;
				}
			} else {
				runFrame = ((int) (runTime / 20)) % 2;
				if (Math.abs(xa) > 10)
					runFrame += 2;
				if (!onGround) {
					if (Math.abs(xa) > 10)
						runFrame = 5;
					else
						runFrame = 4;
				}
			}

			if (onGround
					&& ((facing == -1 && xa > 0) || (facing == 1 && xa < 0))) {
				if (xa > 1 || xa < -1)
					runFrame = large ? 9 : 7;

			}

			if (large) {
				if (ducking)
					runFrame = 14;
				height = ducking ? 12 : 24;
			} else {
				height = 12;
			}

			xPic = runFrame;
		}

		public MarioState(MarioState state) {
			parent = state;
			jT = state.jT;
			yaa = 1 * 3;
			xPic = state.xPic;
			yPic = state.yPic;
			hPic = state.hPic;
			wPic = state.wPic;
			xPicO = state.xPicO;
			yPicO = state.yPicO;
			sheet = state.sheet;
			fire = state.fire;

			mayJump = state.mayJump;
			xJumpSpeed = state.xJumpSpeed;
			yJumpSpeed = state.yJumpSpeed;
			large = state.large;
			ducking = state.ducking;
			jumpTime = state.jumpTime;
			xPic = state.xPic;
			canJump = state.canJump;
			x = state.x;
			y = state.y;
			xa = state.xa;
			ya = state.ya;
			keys = new boolean[state.keys.length];
			facing = state.facing;
			ableToShoot = state.ableToShoot;
			onGround = state.onGround;
			sliding = state.sliding;
			levelScene = state.levelScene;
			storedKeys = new ArrayList<boolean[]>(state.storedKeys);
			allStates = new ArrayList<MoveState>(state.allStates);
			if (state.states != null) {
				allStates.add(MoveState.NewIteration);
				allStates.addAll(state.states);
			}
			xCam = state.xCam;
			yCam = state.yCam;
			lastScore = state.lastScore;

			// score= state.score;
		}

		public MarioState(Mario state) {
			parent = null;
			allStates = new ArrayList<MoveState>();
			jT = state.jT;
			mayJump = state.mayJump;
			xJumpSpeed = state.xJumpSpeed;
			large = state.large;
			yJumpSpeed = state.yJumpSpeed;
			ducking = state.ducking;
			jumpTime = state.jumpTime;
			canJump = state.canJump;

			xPic = state.xPic;
			yPic = state.yPic;
			hPic = state.hPic;
			wPic = state.wPic;
			xPicO = state.xPicO;
			yPicO = state.yPicO;
			sheet = state.sheet;
			fire = state.fire;

			x = state.x;
			y = state.y;
			xa = state.xa;
			ya = state.ya;
			keys = new boolean[state.keys.length];
			facing = state.facing;
			ableToShoot = state.ableToShoot;
			onGround = state.onGround;
			sliding = state.sliding;
			levelScene = Mario.levelScene;
			xCam = levelScene.xCam;
			yCam = levelScene.xCam;
			storedKeys = new ArrayList<boolean[]>();
		}

		public List<MoveState> move() {

			List<MoveState> ar = new ArrayList<MoveState>();
			boolean wasOnGround = onGround;
			float sideWaysSpeed = keys[Mario.KEY_SPEED] ? 1.2f : 0.6f;

			// float sideWaysSpeed = onGround ? 2.5f : 1.2f;

			if (onGround) {
				ducking = keys[Mario.KEY_DOWN] && large;
			}

			if (xa > 2) {
				facing = 1;
			}
			if (xa < -2) {
				facing = -1;
			}
			if (keys[Mario.KEY_SPEED]) {
				ar.add(MoveState.Running);
			}

			// float Wind = 0.2f;
			// float windAngle = 180;
			// xa += Wind * Math.cos(windAngle * Math.PI / 180);

			if (keys[Mario.KEY_JUMP] || (jumpTime < 0 && !onGround && !sliding)) {
				if (jumpTime < 0) {
					xa = xJumpSpeed;
					ya = -jumpTime * yJumpSpeed;
					jumpTime++;
				} else if (onGround && mayJump) {
					xJumpSpeed = 0;
					yJumpSpeed = -1.9f;
					jumpTime = (int) jT;
					ya = jumpTime * yJumpSpeed;
					onGround = false;
					sliding = false;
				} else if (sliding && mayJump) {
					xJumpSpeed = -facing * 6.0f;
					yJumpSpeed = -2.0f;
					jumpTime = -6;
					xa = xJumpSpeed;
					ya = -jumpTime * yJumpSpeed;
					onGround = false;
					sliding = false;
					facing = -facing;
				} else if (jumpTime > 0) {
					xa += xJumpSpeed;
					ya = jumpTime * yJumpSpeed;
					jumpTime--;
				}
			} else {
				jumpTime = 0;
			}

			if (keys[Mario.KEY_LEFT] && !ducking) {
				if (facing == 1)
					sliding = false;
				xa -= sideWaysSpeed;
				if (jumpTime >= 0)
					facing = -1;
			}

			if (keys[Mario.KEY_RIGHT] && !ducking) {
				if (facing == -1)
					sliding = false;
				xa += sideWaysSpeed;
				if (jumpTime >= 0)
					facing = 1;
			}

			if ((!keys[Mario.KEY_LEFT] && !keys[Mario.KEY_RIGHT]) || ducking
					|| ya < 0 || onGround) {
				sliding = false;
			}

			if (keys[Mario.KEY_SPEED] && ableToShoot && Mario.fire
					&& levelScene.fireballsOnScreen < 2) {
			}

			// if (cheatKeys[KEY_LIFE_UP])
			// this.lives++;

			// if (keys[KEY_DUMP_CURRENT_WORLD])
			// try {
			// System.out.println("DUMP:");
			// // levelScene.getObservationStrings(System.out);
			// //levelScene.level.save(System.out);
			// System.out.println("DUMPED:");
			// } catch (IOException e) {
			// e.printStackTrace(); //To change body of catch statement use File
			// | Settings | File Templates.
			// }
			ableToShoot = !keys[Mario.KEY_SPEED];

			mayJump = (onGround || sliding) && !keys[Mario.KEY_JUMP];

			xFlipPic = facing == -1;

			runTime += (Math.abs(xa)) + 5;
			if (Math.abs(xa) < 0.5f) {
				runTime = 0;
				xa = 0;
			}

			calcPic();

			if (sliding) {
				ya *= 0.5f;
			}

			onGround = false;
			move(xa, 0);
			move(0, ya);
			;

			/*
			 * boolean bad = true; for (int jc = (int) (y /
			 * LevelScene.cellSize); jc < levelScene.level.height + 1; jc++) {
			 * if (! levelScene.level.isBlocking((int) (x / 16), jc, (float) 0,
			 * (float) 0)) { bad = false; break; } }
			 */boolean bad = true;
			for (int jc = (int) (y / LevelScene.cellSize); jc < levelScene.level.height + 1; jc++) {
				if (!levelScene.level.isBlocking((int) (x / 16), jc, (float) 0,
						(float) 0)) {
					bad = false;
					break;
				}
			}
			if (bad)
				ar.add(MoveState.NothingBelow);
			// if (!levelScene.level.isBlocking((int) (x / 16),
			// levelScene.level.height, (float) 0,(float) 0)) {
			// ar.add(MoveState.NothingBelow);

			// }

			// if (bad)
			// ar.add(MoveState.NothingBelow);

			// if (levelScene.level.isBlocking((int) ((x) / 16) + 1,
			// (int) (y / LevelScene.cellSize), (float) 0, (float) 0))
			// ar.add(MoveState.TooClose);

			if (y > (levelScene.level.height - 1) * LevelScene.cellSize
					+ LevelScene.cellSize)
				ar.add(MoveState.CriticalFail);

			if (x < 0) {
				x = 0f;
				xa = 0;
			}

			if (x > levelScene.level.length * LevelScene.cellSize) {
				x = new Float(levelScene.level.length * LevelScene.cellSize);
				xa = 0;
			}

			ya *= 0.85f;
			if (onGround) {
				xa *= (GROUND_INERTIA);
			} else {
				xa *= (AIR_INERTIA);
			}

			// if /

			if (!onGround) {
				// ya += 3;
				ya += yaa;
			}
			return ar;

		}

		protected static final float GROUND_INERTIA = 0.89f;
		protected static final float AIR_INERTIA = 0.89f;

		private boolean move(float xa, float ya) {
			while (xa > 8) {
				if (!move(8, 0))
					return false;
				xa -= 8;
			}
			while (xa < -8) {
				if (!move(-8, 0))
					return false;
				xa += 8;
			}
			while (ya > 8) {
				if (!move(0, 8))
					return false;
				ya -= 8;
			}
			while (ya < -8) {
				if (!move(0, -8))
					return false;
				ya += 8;
			}

			boolean collide = false;
			if (ya > 0) {
				if (isBlocking(x + xa - width, y + ya, xa, 0))
					collide = true;
				else if (isBlocking(x + xa + width, y + ya, xa, 0))
					collide = true;
				else if (isBlocking(x + xa - width, y + ya + 1, xa, ya))
					collide = true;
				else if (isBlocking(x + xa + width, y + ya + 1, xa, ya))
					collide = true;
			}
			if (ya < 0) {
				if (isBlocking(x + xa, y + ya - height, xa, ya))
					collide = true;
				else if (collide
						|| isBlocking(x + xa - width, y + ya - height, xa, ya))
					collide = true;
				else if (collide
						|| isBlocking(x + xa + width, y + ya - height, xa, ya))
					collide = true;
			}
			if (xa > 0) {
				sliding = true;
				if (isBlocking(x + xa + width, y + ya - height, xa, ya))
					collide = true;
				else
					sliding = false;
				if (isBlocking(x + xa + width, y + ya - height / 2, xa, ya))
					collide = true;
				else
					sliding = false;
				if (isBlocking(x + xa + width, y + ya, xa, ya))
					collide = true;
				else
					sliding = false;
			}
			if (xa < 0) {
				sliding = true;
				if (isBlocking(x + xa - width, y + ya - height, xa, ya))
					collide = true;
				else
					sliding = false;
				if (isBlocking(x + xa - width, y + ya - height / 2, xa, ya))
					collide = true;
				else
					sliding = false;
				if (isBlocking(x + xa - width, y + ya, xa, ya))
					collide = true;
				else
					sliding = false;
			}

			if (collide) {
				if (xa < 0) {
					x = new Float((int) ((x - width) / 16) * 16 + width);
					this.xa = 0;
				}
				if (xa > 0) {
					x = new Float((int) ((x + width) / 16 + 1) * 16 - width - 1);
					this.xa = 0;
				}
				if (ya < 0) {
					y = (int) ((y - height) / 16) * 16 + height;
					jumpTime = 0;
					this.ya = 0;
				}
				if (ya > 0) {
					y = (int) ((y - 1) / 16 + 1) * 16 - 1;
					onGround = true;
				}
				return false;
			} else {
				x += xa;
				y += ya;
				return true;
			}
		}

		private boolean isBlocking(final float _x, final float _y,
				final float xa, final float ya) {
			int x = (int) (_x / 16);
			int y = (int) (_y / 16);
			if (x == (int) (this.x / 16) && y == (int) (this.y / 16))
				return false;

			boolean blocking = levelScene.level.isBlocking(x, y, xa, ya);

			byte block = levelScene.level.getBlock(x, y);

			if (((Level.TILE_BEHAVIORS[block & 0xff]) & Level.BIT_PICKUPABLE) > 0) {
			}

			if (blocking && ya < 0) {
				// levelScene.bump(x, y, large);
			}

			return blocking;
		}

		private boolean isWorseBlocking(final float _x, final float _y) {
			int x1 = (int) ((_x) / 16);
			int y1 = (int) ((_y) / 16);

			boolean blocking = isWorseBlocking(enemiesFloatPos, x1, y1);

			return blocking;
		}

		public boolean isWorseBlocking(float[] enemiesFloatPos, int x, int y) {

			boolean done = (getEnemy(enemiesFloatPos, x, y));
			done = done || (getEnemy(enemiesFloatPos, x - 1, y));
			done = done || (getEnemy(enemiesFloatPos, x + 1, y));
			done = done || (getEnemy(enemiesFloatPos, x, y - 1));
			done = done || (getEnemy(enemiesFloatPos, x, y + 1));

			return done;

		}

		private boolean getEnemy(float[] enemiesFloatPos, int i, int j) {
			if (i < 0 || j < 0)
				return false;

			for (int c = 0; c < enemiesFloatPos.length; c += 3) {
				if ((enemiesFloatPos[c + 1] > (i) * 16 && enemiesFloatPos[c + 1] < (i + 1) * 16)
						|| (enemiesFloatPos[c + 2] > (j) * 16 && enemiesFloatPos[c + 2] < (j + 1) * 16)) {

					return true;
				}
			}

			return false;
		}

		public boolean equals(Object o) {
			MarioState ms = ((MarioState) o);

			int limit = 2;
			return (Math.abs(ms.x - x) < limit && Math.abs(ms.y - y) < limit);

		}

		@Override
		public int compareTo(MarioState o) {
			return this.score.compareTo(o.score);
		}
	}

}