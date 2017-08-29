/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.config;

import java.util.*;

import android.app.Service;
import android.content.*;
import android.os.IBinder;
import android.os.RemoteException;

import org.geometerplus.zlibrary.core.options.Config;

import org.geometerplus.android.fbreader.api.FBReaderIntents;

public final class ConfigShadow extends Config {
	private final Context myContext;
	private volatile SQLiteConfig mSqLiteConfig;

	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			try {
				setToCache(
					intent.getStringExtra("group"),
					intent.getStringExtra("name"),
					intent.getStringExtra("value")
				);
			} catch (Exception e) {
				// ignore
			}
		}
	};

	public ConfigShadow(Context context) {
		myContext = context;
		mSqLiteConfig = new SQLiteConfig(context);
	}

	@Override
	public boolean isInitialized() {
		return mSqLiteConfig != null;
	}

	@Override
	public void runOnConnect(Runnable runnable) {
		if (mSqLiteConfig != null) {
			runnable.run();
		}
	}

	@Override
	public List<String> listGroups() {
		if (mSqLiteConfig == null) {
			return Collections.emptyList();
		}
			return mSqLiteConfig.listGroups();
	}

	@Override
	public List<String> listNames(String group) {
		if (mSqLiteConfig == null) {
			return Collections.emptyList();
		}
			return mSqLiteConfig.listNames(group);
	}

	@Override
	public void removeGroup(String name) {
		if (mSqLiteConfig != null) {
				mSqLiteConfig.removeGroup(name);
		}
	}

	public boolean getSpecialBooleanValue(String name, boolean defaultValue) {
		return myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
			.getBoolean(name, defaultValue);
	}

	public void setSpecialBooleanValue(String name, boolean value) {
		myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE).edit()
			.putBoolean(name, value).commit();
	}

	public String getSpecialStringValue(String name, String defaultValue) {
		return myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
			.getString(name, defaultValue);
	}

	public void setSpecialStringValue(String name, String value) {
		myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE).edit()
			.putString(name, value).commit();
	}

	@Override
	protected String getValueInternal(String group, String name) throws NotAvailableException {
		if (mSqLiteConfig == null) {
			throw new NotAvailableException("Config is not initialized for " + group + ":" + name);
		}
			return mSqLiteConfig.getValue(group, name);
	}

	@Override
	protected void setValueInternal(String group, String name, String value) {
		if (mSqLiteConfig != null) {
				mSqLiteConfig.setValue(group, name, value);
		}
	}

	@Override
	protected void unsetValueInternal(String group, String name) {
		if (mSqLiteConfig != null) {
				mSqLiteConfig.unsetValue(group, name);
		}
	}

	@Override
	protected Map<String,String> requestAllValuesForGroupInternal(String group) throws NotAvailableException {
		if (mSqLiteConfig == null) {
			throw new NotAvailableException("Config is not initialized for " + group);
		}
			final Map<String,String> values = new HashMap<String,String>();
			for (String pair : mSqLiteConfig.requestAllValuesForGroup(group)) {
				final String[] split = pair.split("\000");
				switch (split.length) {
					case 1:
						values.put(split[0], "");
						break;
					case 2:
						values.put(split[0], split[1]);
						break;
				}
			}
			return values;
	}
}
