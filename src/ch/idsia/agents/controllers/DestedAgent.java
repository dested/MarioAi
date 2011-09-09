/*
 * Copyright (c) 2009-2010, Sergey Karakovskiy and Julian Togelius
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Mario AI nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ch.idsia.agents.controllers;

import java.util.ArrayList;
import java.util.List;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.engine.level.Level;
import ch.idsia.benchmark.mario.engine.sprites.Fireball;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sparkle;
import ch.idsia.benchmark.mario.environments.Environment;

public class DestedAgent extends BasicMarioAIAgent implements Agent {
	int trueJumpCounter = 0;
	int trueSpeedCounter = 0;

	public DestedAgent() {
		super("ForwardAgent");
		reset();
	}

	public void reset() {
		action = new boolean[Environment.numberOfKeys];
		trueJumpCounter = 0;
		trueSpeedCounter = 0;
	}

	public boolean[] getAction() {
		// action = new boolean[Environment.numberOfKeys];
		// this Agent requires observation integrated in advance.

		return make();

	}

	private boolean[] make() {
		MarioState original = new MarioState(Mario.instance);

		int[][] keyz = new int[][] { new int[] { Mario.KEY_DOWN },
				new int[] { Mario.KEY_JUMP, Mario.KEY_SPEED },
				new int[] { Mario.KEY_LEFT, Mario.KEY_SPEED },
				new int[] { Mario.KEY_RIGHT, Mario.KEY_SPEED },
				new int[] { Mario.KEY_UP, Mario.KEY_SPEED },
				new int[] { Mario.KEY_JUMP }, new int[] { Mario.KEY_LEFT },
				new int[] { Mario.KEY_RIGHT }, new int[] { Mario.KEY_UP } };

		List<MarioState> mz = new ArrayList<MarioState>();
		mz.add(original);
		int limit = 1000;
		for (int j = 0; j < 6; j++) {
			List<MarioState> mzc = new ArrayList<MarioState>();
			for (MarioState marioState : mz) {
				for (int i = 0; i < keyz.length; i++) {
					MarioState m = new MarioState(marioState);
					// maybe =false
					for (int ic = 0; ic < m.keys.length; ic++)
						m.keys[ic] = false;
					for (int js : keyz[i]) {
						m.keys[js] = true;
					}
					if (j == 0)
						m.setOriginalKeys();
					int nScore;
					m.score += nScore = doit(m);
					if (nScore < limit)
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

	public int doit(MarioState ms) {
		int score = 0;

		float sideWaysSpeed = ms.keys[Mario.KEY_SPEED] ? 1.2f : 0.6f;
		if (!ms.keys[Mario.KEY_SPEED]) {
			score += 500;
		}

		if (ms.xa > 2) {
			ms.facing = 1;
			score-= 100;
		}
		if (ms.xa < -2) {
			ms.facing = -1;
			score += 30;
		}

		if (ms.keys[Mario.KEY_JUMP]
				|| (ms.jumpTime < 0 && !ms.onGround && !ms.sliding)) {

			score -= 300;
			if (ms.jumpTime < 0) {
				ms.xa = ms.xJumpSpeed;
				ms.ya = -ms.jumpTime * ms.yJumpSpeed;
				ms.jumpTime++;
				score -= 100;
			} else if (ms.onGround && ms.mayJump) {
				ms.xJumpSpeed = 0;
				ms.yJumpSpeed = -1.9f;
				ms.jumpTime = (int) ms.jT;
				ms.ya = ms.jumpTime * ms.yJumpSpeed;
				ms.onGround = false;
				ms.sliding = false;
				score -= 200;
			} else if (ms.sliding && ms.mayJump) {
				ms.xJumpSpeed = -ms.facing * 6.0f;
				ms.yJumpSpeed = -2.0f;
				ms.jumpTime = -6;
				ms.xa = ms.xJumpSpeed;
				ms.ya = -ms.jumpTime * ms.yJumpSpeed;
				ms.onGround = false;
				ms.sliding = false;
				ms.facing = -ms.facing;
				score -= 1000;
			} else if (ms.jumpTime > 0) {
				ms.xa += ms.xJumpSpeed;
				ms.ya = ms.jumpTime * ms.yJumpSpeed;
				ms.jumpTime--;
				score -= 200;
			}
		} else {
			ms.jumpTime = 0;

			score += 100;
		}

		if (ms.keys[Mario.KEY_LEFT] && !ms.ducking) {
			if (ms.facing == 1)
				ms.sliding = false;
			ms.xa -= sideWaysSpeed;
			if (ms.jumpTime >= 0)
				ms.facing = -1;
			score += 50;
		}

		if (ms.keys[Mario.KEY_RIGHT] && !ms.ducking) {
			if (ms.facing == -1)
				ms.sliding = false;
			ms.xa += sideWaysSpeed;
			if (ms.jumpTime >= 0)
				ms.facing = 1;
			score -= 50;
		}

		if ((!ms.keys[Mario.KEY_LEFT] && !ms.keys[Mario.KEY_RIGHT])
				|| ms.ducking || ms.ya < 0 || ms.onGround) {
			ms.sliding = false;
			score -= 50;
		}

		ms.ableToShoot = !ms.keys[Mario.KEY_SPEED];

		ms.mayJump = (ms.onGround || ms.sliding) && !ms.keys[Mario.KEY_JUMP];

		score -= ms.xa * 5;
		score -= ms.ya * 2;
		if (ms.xa == 0) {
			score += 100000;
		}

		boolean didCollide = ms.move(ms.xa, 0);
		didCollide = ms.move(0, ms.ya) ? true : didCollide;

		if (ms.y > ms.levelScene.level.height * LevelScene.cellSize
				+ LevelScene.cellSize)
			score+=Integer.MAX_VALUE;


		if (didCollide && !ms.sliding) {
			score += 400;
		}
		if(ms.sliding)score-=200;
		return score;
	}

	public class MarioState {
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
		}

		public MarioState(Mario state) {
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
	}

}