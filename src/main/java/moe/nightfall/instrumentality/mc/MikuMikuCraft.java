package moe.nightfall.instrumentality.mc;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(name = "MikuMikuCraft", modid = "instrumentality")
public class MikuMikuCraft {
	
	@SidedProxy(serverSide = "moe.nightfall.instrumentality.mc.CommonProxy", clientSide = "moe.nightfall.instrumentality.mc.ClientProxy")
	public static ClientProxy proxy;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		proxy.preInit();
	}
}
