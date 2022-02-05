/**************************************************************************
 Simple LaTeX filter for OmegaT

 Copyright (C) 2022 Lev Abashkin

 This file is NOT a part of OmegaT.

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
 **************************************************************************/

package com.pilulerouge.omegat.latex;

import gen.core.filters.Filter;
import gen.core.filters.Filters;
import org.omegat.core.Core;
import org.omegat.core.data.IProject;
import org.omegat.filters2.master.FilterMaster;
import org.omegat.util.Log;
import org.omegat.util.StaticUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;



/**
 * Common stuff lives here.
 */
public final class Util {

    private static Map<String, String> filterOptions;

    static final ResourceBundle RB;

    static {
        ResourceBundle.Control utf8Control = new UTF8Control();
        RB = ResourceBundle.getBundle("strings", Locale.getDefault(), utf8Control);
    }

    static final String FILTER_NAME = RB.getString("FILTER_NAME");

    static void logLocalRB(String key, Object... parameters) {
        MessageFormat formatter = new MessageFormat(RB.getString(key));
        Log.log(formatter.format(parameters));
    }

    /**
     * Load filter options from filters.xml before OmegaT does it for us.
     * @return map of option string values
     */
    static Map<String, String> getFilterOptions() {
        if (filterOptions != null) {
            return filterOptions;
        }

        filterOptions = new HashMap<>();
        File configFile = new File(StaticUtils.getConfigDir(), FilterMaster.FILE_FILTERS);
        if (!configFile.exists()) {
            return filterOptions;
        }
        Filters allFilters;
        try {
            JAXBContext configCtx = JAXBContext.newInstance(Filters.class);
            Unmarshaller unm = configCtx.createUnmarshaller();
            allFilters = (Filters) unm.unmarshal(configFile);
        } catch (Exception e) {
            return filterOptions;
        }
        for (Filter f: allFilters.getFilters()) {
            if (f.getClassName().equals(SimpleLatexFilter.class.getName())) {
                for(Filter.Option opt: f.getOption()) {
                    filterOptions.put(opt.getName(), opt.getValue());
                }
            }
        }
        return filterOptions;
    }

    static boolean currentlyUsingThisFilter() {
        String filePath = Core.getEditor().getCurrentFile();
        if (filePath == null) {
            return false;
        }
        for (IProject.FileInfo fi : Core.getProject().getProjectFiles()) {
            if (fi.filePath.equals(filePath)
                    && fi.filterFileFormatName.equals(FILTER_NAME)) {
                return true;
            }
        }
        return false;
    }

    private Util() {
    }
}
