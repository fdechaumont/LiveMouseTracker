package plugins.fab.azure.kinect;

import java.util.List;

public interface DatasetReadyListener
{
    void DatasetReceived(List<Dataset> datasets);
}
