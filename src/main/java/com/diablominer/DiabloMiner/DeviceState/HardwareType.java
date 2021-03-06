/*
 * DiabloMiner - OpenCL miner for Bitcoin
 * Copyright (C) 2010, 2011, 2012 Patrick McFarland <diablod3@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	If not, see <http://www.gnu.org/licenses/>.
 */

package com.diablominer.DiabloMiner.DeviceState;

import com.diablominer.DiabloMiner.DiabloMiner;
import com.diablominer.DiabloMiner.DiabloMinerFatalException;

import java.util.List;

abstract public class HardwareType {
	DiabloMiner diabloMiner;

	List<? extends DeviceState> deviceStates = null;

	public HardwareType(DiabloMiner diabloMiner) throws DiabloMinerFatalException {
		this.diabloMiner = diabloMiner;
   }

	abstract public List<? extends DeviceState> getDeviceStates();

	public DiabloMiner getDiabloMiner() {
		return diabloMiner;
	}
}
