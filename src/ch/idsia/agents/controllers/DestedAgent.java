package ch.idsia.agents.controllers;

import java.util.ArrayList;
import java.util.List;

import com.sun.org.apache.bcel.internal.generic.LLOAD;

import ch.idsia.agents.Agent;
import ch.idsia.agents.controllers.DestedAgent.MoveState;
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

	}

	public boolean[] getAction() {
		// action = new boolean[Environment.numberOfKeys];
		// this Agent requires observation integrated in advance.

		return make();

	}

	private boolean[] make() {
		MarioState original = new MarioState(Mario.instance);

		int[][] keyz = new int[][] {
				new int[] { Mario.KEY_JUMP, Mario.KEY_SPEED },
				new int[] { Mario.KEY_LEFT, Mario.KEY_SPEED },
				new int[] { Mario.KEY_RIGHT, Mario.KEY_SPEED },
				new int[] { Mario.KEY_LEFT, Mario.KEY_SPEED, Mario.KEY_JUMP },
				new int[] { Mario.KEY_RIGHT, Mario.KEY_SPEED, Mario.KEY_JUMP },
				new int[] { Mario.KEY_JUMP }, new int[] { Mario.KEY_LEFT },
				new int[] { Mario.KEY_RIGHT } };

		List<MarioState> mz = new ArrayList<MarioState>();
		mz.add(original);
		int limit = 1000;
		for (int j = 0; j < 10; j++) {
			List<MarioState> mzc = new ArrayList<MarioState>();
			if(mz.size()>j*100000)
			for (MarioState marioState : mz) {
				for (int i = 0; i < keyz.length; i++) {
					MarioState m = new MarioState(marioState);
					// maybe =false
					for (int js : keyz[i]) {
						m.keys[js] = true;
					}
					if (j == 0)
						m.setOriginalKeys();
					int nScore = 0;

					List<MoveState> fs = doit(m);
					m.states = fs;
					for (MoveState moveState : fs) {
						switch (moveState) {
						case Collided:
							nScore += 80;
							break;
						case CriticalFail:
							nScore += limit * 2;
							break;
						case Jumping:
							nScore -= 80;
							break;
						case JumpingWasSliding:
							nScore -= 40;
							break;
						case MovingAway:
							nScore += 10;
							break;
						case MovingTowards:
							nScore -= 60;
							break;
						case NotRunning:
							nScore += 150;
							break;
						case Running:
							nScore -= 80;
							break;
						case NothingBelow:
							nScore += limit * 2;
						case Sliding:
							nScore -= ((limit * 2)+100);
							break;
						case Stopped:
							nScore +=50;
							break;
						}
					}

					nScore -= (m.x - original.x)*20 ;

//					nScore+=(j*30);
					m.score += nScore;
					if (nScore < (-20))
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
			return curState.originalKeys == null ? curState.keys
					: curState.originalKeys;
		}
		return new boolean[6];
	}

	public enum MoveState {
		CriticalFail,NothingBelow, Running, NotRunning, Jumping, MovingAway, MovingTowards, JumpingWasSliding, Stopped, Sliding, Collided, NewIteration

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
		}
		if (ms.xa < -2) {
			ms.facing = -1;
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

		 

		float oldx = ms.x;
		float oldy = ms.y;

		float running = 4;

		boolean didntCollide = ms.move(ms.xa, 0);
		didntCollide = ms.move(0, ms.ya) ? true : didntCollide;
		if (Math.abs(ms.x - oldx) > running) {
			mc.add(MoveState.Running);
		}

		if (Math.floor(ms.x) > Math.floor(oldx))
			mc.add(MoveState.MovingTowards);
		else if (Math.floor(ms.x) < Math.floor(oldx))
			mc.add(MoveState.MovingAway);
		else if (Math.floor(ms.x) == Math.floor(oldx))
			if(!ms.sliding)
				mc.add(MoveState.Stopped);

		boolean bad=true;
		for(int jc=(int)(ms.y/LevelScene.cellSize);jc<ms.levelScene.level.height+1;jc++){
			if(!ms.levelScene.level.isBlocking((int)(ms.x/16),jc,(float)0,(float)0)){
				bad=false;
				break;
			}
		}
		if(bad)
			mc.add(MoveState.NothingBelow);
		
		if (ms.y > ms.levelScene.level.height * LevelScene.cellSize
				+ LevelScene.cellSize)
			mc.add(MoveState.CriticalFail);

		if (!didntCollide && !ms.sliding) {
			mc.add(MoveState.Collided);
		}
		if (ms.sliding)
			mc.add(MoveState.Sliding);

		return mc;
	}

	public class MarioState {
		public List<MoveState> states;
		public List<MoveState> allStates;
		public boolean[] originalKeys;
		public int score;
		public float jT;
		public boolean mayJump;
		public float xJumpSpeed;
		public float yJumpSpeed;
		public boolean ducking;
		public int jumpTime;
		boolean canJump;
		float x;
		float y;
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

		public MarioState() {
			keys = new boolean[7];
		}

		public void setOriginalKeys() {
			originalKeys = keys;

		}

		public MarioState(MarioState state) {
			jT = state.jT;
			mayJump = state.mayJump;
			xJumpSpeed = state.xJumpSpeed;
			yJumpSpeed = state.yJumpSpeed;
			ducking = state.ducking;
			jumpTime = state.jumpTime;
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
			originalKeys = state.originalKeys;
			allStates = new ArrayList<MoveState>(state.allStates);
			if (state.states != null)
				{allStates.add(MoveState.NewIteration);
				allStates.addAll(state.states);}
			xCam = state.xCam;
			yCam = state.yCam;
		}

		public MarioState(Mario state) {
			allStates = new ArrayList<MoveState>();
			jT = state.jT;
			mayJump = state.mayJump;
			xJumpSpeed = state.xJumpSpeed;
			yJumpSpeed = state.yJumpSpeed;
			ducking = state.ducking;
			jumpTime = state.jumpTime;
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
			levelScene = Mario.levelScene;
			xCam = levelScene.xCam;
			yCam = levelScene.xCam;
		}

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
					x = (int) ((x - width) / 16) * 16 + width;
					this.xa = 0;
				}
				if (xa > 0) {
					x = (int) ((x + width) / 16 + 1) * 16 - width - 1;
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

			return blocking;
		}

		private boolean isWorseBlocking(final float _x, final float _y) {
			int x1 = (int) ((_x ) / 16);
			int y1 = (int) ((_y ) / 16);

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
				if ((enemiesFloatPos[c + 1] > (i ) * 16 && enemiesFloatPos[c + 1] < (i + 1) * 16) || (enemiesFloatPos[c + 2] > (j ) * 16 && enemiesFloatPos[c + 2] <(j + 1) * 16)) {
					
					return true;
				}
			}

			return false;
		}
	}

}