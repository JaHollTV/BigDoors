package nl.pim16aap2.bigdoors.gui;

import nl.pim16aap2.bigdoors.util.PageType;

interface IGUIPage
{
    public void handleInput(int interactionIDX);

    public void refresh();

    public PageType getPageType();
}
