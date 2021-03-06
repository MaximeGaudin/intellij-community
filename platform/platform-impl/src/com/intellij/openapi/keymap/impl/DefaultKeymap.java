/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.keymap.impl;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.SystemInfo;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Belyaev
 */
public class DefaultKeymap {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.keymap.impl.DefaultKeymap");

  @NonNls
  private static final String KEY_MAP = "keymap";
  @NonNls
  private static final String NAME_ATTRIBUTE = "name";

  private final ArrayList<Keymap> myKeymaps = new ArrayList<Keymap>();

  public static DefaultKeymap getInstance() {
    return ServiceManager.getService(DefaultKeymap.class);
  }

  public DefaultKeymap() {
    for(BundledKeymapProvider provider: getProviders()) {
      final List<String> fileNames = provider.getKeymapFileNames();
      for (String fileName : fileNames) {
        try {
          final Document document = JDOMUtil.loadResourceDocument(new URL("file:///idea/" + fileName));
          loadKeymapsFromElement(document.getRootElement());
        }
        catch (Exception e) {
          LOG.error(e);
        }
      }
    }
  }

  protected BundledKeymapProvider[] getProviders() {
    return Extensions.getExtensions(BundledKeymapProvider.EP_NAME);
  }

  private void loadKeymapsFromElement(final Element element) throws InvalidDataException {
    @SuppressWarnings("unchecked") final List<Element> children = (List<Element>)element.getChildren();
    for (Element child : children) {
      if (KEY_MAP.equals(child.getName())) {
        String keymapName = child.getAttributeValue(NAME_ATTRIBUTE);
        DefaultKeymapImpl keymap = keymapName.startsWith(KeymapManager.MAC_OS_X_KEYMAP) ? new MacOSDefaultKeymap() : new DefaultKeymapImpl();
        keymap.readExternal(child, myKeymaps.toArray(new Keymap[myKeymaps.size()]));
        keymap.setName(keymapName);
        myKeymaps.add(keymap);
      }
    }
  }

  public Keymap[] getKeymaps() {
    return myKeymaps.toArray(new Keymap[myKeymaps.size()]);
  }

  public String getDefaultKeymapName() {
    if (SystemInfo.isMac) {
      return KeymapManager.MAC_OS_X_KEYMAP;
    }
    else if (SystemInfo.isLinux) {
      return KeymapManager.X_WINDOW_KEYMAP;
    }
    else {
      return KeymapManager.DEFAULT_IDEA_KEYMAP;
    }
  }

  public String getKeymapPresentableName(KeymapImpl keymap) {
    String name = keymap.getName();
    return KeymapManager.DEFAULT_IDEA_KEYMAP.equals(name) ? "Default" : name;
  }
}
