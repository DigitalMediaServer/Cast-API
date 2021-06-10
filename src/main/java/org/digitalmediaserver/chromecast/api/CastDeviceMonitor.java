/*
 * Copyright 2014 Vitaly Litvak (vitavaque@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.digitalmediaserver.chromecast.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Utility class that discovers cast devices and holds references to all
 * of them.
 */
public final class CastDeviceMonitor {

	@Nonnull
	protected final MulticastDNSServiceListener listener = new MulticastDNSServiceListener();

	@Nonnull
	protected final Object lock = new Object();

	@Nullable
	@GuardedBy("lock")
	protected JmDNS mDNS; //TODO: (Nad) Sync

	@Nonnull
	@GuardedBy("lock")
	protected final Set<DeviceDiscoveryListener> listeners = new LinkedHashSet<>();

	@Nonnull
	@GuardedBy("lock")
	protected final Set<CastDevice> chromeCasts = new LinkedHashSet<>();

	/**
	 * Creates a new instance. Use {@link #startDiscovery()} to start
	 * monitoring.
	 */
	public CastDeviceMonitor() {
	}

	/**
	 * Returns a copy of the currently seen chrome casts.
	 *
	 * @return a copy of the currently seen chromecast devices.
	 */
	public Set<CastDevice> getCastDevices() { //TODO: (Nad) JavaDocs are koko
		return new LinkedHashSet<>(chromeCasts);
	}

	/**
	 * Service listener to receive mDNS service updates.
	 */
	public class MulticastDNSServiceListener implements ServiceListener { //TODO: (Nad) Move

		@Override
		public void serviceAdded(ServiceEvent se) { //TODO: (Nad) Move to registered..?
			if (se.getInfo() != null) {
				CastDevice device = new CastDevice(mDNS, se.getInfo().getName(), true); //TODO: (Nad) Check if it's already registered first
				chromeCasts.add(device);
				for (DeviceDiscoveryListener listener : listeners) {
					listener.deviceDiscovered(device);
				}
			}
		}

		@Override
		public void serviceRemoved(ServiceEvent se) {
			if (CastDevice.SERVICE_TYPE.equals(se.getType())) {
				// We have a ChromeCast device unregistering
				Set<CastDevice> copy = getCastDevices();
				CastDevice deviceRemoved = null;
				// Probably better keep a map to better lookup devices
				for (CastDevice device : copy) {
					if (device.getDNSName().equals(se.getInfo().getName())) { //TODO: (Nad) Fix
						deviceRemoved = device;
						chromeCasts.remove(device);
						break;
					}
				}
				if (deviceRemoved != null) {
					for (DeviceDiscoveryListener listener : listeners) {
						listener.deviceRemoved(deviceRemoved);
					}
				}
			}
		}

		@Override
		public void serviceResolved(ServiceEvent se) {
			// intentionally blank
		}
	}

	/**
	 * Starts discovery of cast devices.
	 */
	public void startDiscovery() throws IOException { //TODO: (Nad) Doc implications
		startDiscovery(null, null);
	}

	/**
	 * Starts discovery of cast devices.
	 *
	 * @param addr the address of the network interface that should be used for
	 *            discovery.
	 */
	public void startDiscovery(@Nullable InetAddress addr, @Nullable String name) throws IOException {
		synchronized (lock) {
			if (mDNS == null) {
				mDNS = JmDNS.create(addr, name);
				mDNS.addServiceListener(CastDevice.SERVICE_TYPE, listener);
			}
		}
	}

	/**
	 * Stops discovery of cast devices and removes all discovered devices.
	 */
	public void stopDiscovery() throws IOException {
		stopDiscovery(false);
	}

	/**
	 * Stops discovery of cast devices and removes all discovered devices.
	 *
	 * @param notifyListeners if {@code true}, also notifies listeners that the
	 *            devices are removed, to trigger potential cleanup.
	 */
	public void stopDiscovery(boolean notifyListeners) throws IOException {
		Set<CastDevice> tmpDevices = null;
		Set<DeviceDiscoveryListener> tmpListeners = null;
		synchronized (lock) {
			if (mDNS != null) { //TODO: (Nad) Clear / send events..?
				mDNS.close();
				mDNS = null;
			}
			if (notifyListeners) {
				tmpDevices = new LinkedHashSet<>(chromeCasts);
				tmpListeners = new LinkedHashSet<>(listeners);
			}
			chromeCasts.clear();
		}

		if (tmpDevices != null && !tmpDevices.isEmpty() && tmpListeners != null && !tmpListeners.isEmpty()) {
			for (CastDevice device : tmpDevices) {
				for (DeviceDiscoveryListener listener : tmpListeners) {
					listener.deviceRemoved(device);
				}
			}
		}
	}

	public boolean registerListener(@Nullable DeviceDiscoveryListener listener) {
		if (listener == null) {
			return false;
		}
		synchronized (lock) {
			return listeners.add(listener);
		}
	}

	public boolean unregisterListener(@Nullable DeviceDiscoveryListener listener) {
		if (listener == null) {
			return false;
		}
		synchronized (lock) {
			return listeners.remove(listener);
		}
	}
}
