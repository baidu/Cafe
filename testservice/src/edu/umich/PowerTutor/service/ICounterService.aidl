/*
Copyright (C) 2011 The University of Michigan

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

Please send inquiries to powertutor@umich.edu
*/

package edu.umich.PowerTutor.service;

interface ICounterService {
  // Returns the name of the components that are being logged.
  String[] getComponents();

  // Returns the maximum power usage for each of the components being logged.
  int[] getComponentsMaxPower();

  // Returns a bit mask with a 1 in the ith bit if component i doesn't have
  // uid specific information.
  int getNoUidMask();

  // Returns the power consumption in mW for component componentId for the last
  // count iterations.  uid can be specified to make this data only include a
  // specific user id or you can provide SystemInfo.AID_ALL to request
  // global power state information.
  int[] getComponentHistory(int count, int componentId, int uid);

  // Returns the total energy consumption for each component in the same order
  // that the components were returned in getComponents() and in the same order
  // that the components are populated by PhoneSelector.generateComponents().
  //
  // uid can be specified to make the information specific to a single user
  // id or SystemInfo.AID_ALL can be specified to request global information.
  //
  // windowType should be one of Counter.WINDOW_MINUTE, Counter.WINDOW_HOUR,
  // Counter.WINDOW_DAY, Counter.WINDOW_TOTAL to request the window that the
  // energy usage will be calculated over.
  //
  // The returned result is given in mJ.
  long[] getTotals(int uid, int windowType);

  // Like getTotals() except that each entry is divided by how long the given
  // uid was running.  If SystemInfo.AID_ALL is provided this is effectively
  // like dividing each entry by the window size. (unless PowerTutor hasn't
  // been running that long).
  long[] getMeans(int uid, int windowType);

  // Gets the total time that this uid has been running in seconds.
  long getRuntime(int uid, int windowType);

  // Returns a byte array representing a serialized array of UidInfo structures.
  // See UidInfo.java for what information is given.  Note that members marked
  // as transient are not filled in.
  // Power contributions from component i will be dropped if the ith bit is set
  // in ignoreMask.  Providing 0 for ignoreMask will give results for all
  // components.
  //
  // Example Usage:
  //   byte[] rawUidInfo = counterService.getUidInfo(windowType);
  //   UidInfo[] uidInfos = (UidInfo[])new ObjectInputStream(
  //       new ByteArrayInputStream(rawUidInfo)).readObject();
  byte[] getUidInfo(int windowType, int ignoreMask);

  // Return miscellaneous data point for the passed uid.
  // Current extras included:
  //   OLEDSCORE
  long getUidExtra(String name, int uid);
  void hello();
}
