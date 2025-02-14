/*
 * Copyright (c) 2014 PikaMug and contributors. All rights reserved.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
 * NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package me.blackvein.quests.reflect.worldguard;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.Association;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.association.Associables;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

public class WorldGuardAPI {
    private Object worldGuard7 = null;
    private WorldGuardPlugin worldGuardPlugin = null;
    private Object regionContainer = null;
    private Method regionContainerGetMethod = null;
    private Class<?> vector = null;
    private Constructor<?> vectorConstructor = null;
    private boolean initialized = false;

    public WorldGuardAPI(final Plugin wg) {
        if (wg instanceof WorldGuardPlugin) {
            worldGuardPlugin = (WorldGuardPlugin)wg;

            try {
                Class.forName("com.sk89q.worldguard.WorldGuard");
                worldGuard7 = WorldGuardAPI_7_0_0.getInstance();
                // WorldGuard 7+
            } catch (final Exception ex) {
                // WorldGuard <7
            }
        }
    }
    
    public boolean isEnabled() {
        return worldGuardPlugin != null;
    }

    protected RegionAssociable getAssociable(final Player player) {
        final RegionAssociable associable;
        if (player == null) {
            associable = Associables.constant(Association.NON_MEMBER);
        } else {
            associable = worldGuardPlugin.wrapPlayer(player);
        }

        return associable;
    }
    
    private void initialize() {
        if (!initialized) {
            initialized = true;
            if (worldGuard7 == null) {
                // WorldGuard <7
                try {
                    final Method getRegionContainerMethod = worldGuardPlugin.getClass().getMethod("getRegionContainer");
                    regionContainer = getRegionContainerMethod.invoke(worldGuardPlugin);
                    regionContainerGetMethod = regionContainer.getClass().getMethod("get", World.class);
                    vector = Class.forName("com.sk89q.worldedit.Vector");
                    vectorConstructor = vector.getConstructor(double.class, double.class, double.class);
                } catch (final Exception e) {
                    Bukkit.getLogger().log(Level.WARNING,
                            "Quests failed to bind to WorldGuard, integration will not work!", e);
                    regionContainer = null;
                    return;
                }
            }

            if (regionContainer == null) {
                Bukkit.getLogger()
                        .warning("Quests failed to find RegionContainer, WorldGuard integration will not function!");
            }
        }
    }

    @Nullable
    public RegionManager getRegionManager(final World world) {
        if (worldGuard7 != null) {
            return WorldGuardAPI_7_0_0.getRegionManager(world);
        } else {
            initialize();
            if (regionContainer == null || regionContainerGetMethod == null) return null;
            RegionManager regionManager = null;
            try {
                regionManager = (RegionManager)regionContainerGetMethod.invoke(regionContainer, world);
            } catch (final Exception e) {
                Bukkit.getLogger().log(Level.WARNING,
                        "Quests encountered an error getting WorldGuard RegionManager", e);
            }
            return regionManager;
        }
    }
    
    public ApplicableRegionSet getApplicableRegions(final World world, final Location location) {
        if (worldGuard7 != null) {
            return WorldGuardAPI_7_0_0.getApplicableRegions(world, location);
        } else {
            initialize();
            final RegionManager regionManager = getRegionManager(world);
            ApplicableRegionSet ars = null;
            if (regionManager == null) return null;
            try {
                final Method getApplicableRegionsMethod = regionManager.getClass()
                        .getMethod("getApplicableRegions", Location.class);
                ars = (ApplicableRegionSet)getApplicableRegionsMethod.invoke(regionManager, location);
            } catch (final Exception e) {
                Bukkit.getLogger().log(Level.WARNING,
                        "Quests encountered an error getting RegionManager#getApplicableRegions", e);
            }
            return ars;
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getApplicableRegionsIDs(final World world, final Location location) {
        if (worldGuard7 != null) {
            return WorldGuardAPI_7_0_0.getApplicableRegionsIDs(world, location);
        } else {
            initialize();
            final RegionManager regionManager = getRegionManager(world);
            List<String> ari = null;
            if (regionManager == null) return null;
            try {
                final Method getApplicableRegionsMethod = regionManager.getClass()
                        .getMethod("getApplicableRegionsIDs", vector);
                ari = (List<String>)getApplicableRegionsMethod.invoke(regionManager, 
                        vectorConstructor.newInstance(location.getX(), location.getY(), location.getZ()));
            } catch (final Exception e) {
                Bukkit.getLogger().log(Level.WARNING,
                        "Quests encountered an error getting RegionManager#getApplicableRegionsIDs", e);
            }
            return ari;
        }
    }
}
