# Akamai EdgeWorkers IntelliJ Plugin

## Install

1. Install IntelliJ IDEA: [https://www.jetbrains.com/idea/download/](https://www.jetbrains.com/idea/download/) or
   WebStorm IDE: [https://www.jetbrains.com/webstorm/](https://www.jetbrains.com/webstorm/)

2. Create or open a Project

3. Open IntelliJ Preferences and go to the Plugins interface:

    <img src="https://lh6.googleusercontent.com/ZFoEyBxrQm31REicT4EwDvCgIRqtKx0pW8xaF5A2YfxdN2VdrhMUsqNXKapIcZle_REigYsLxmmySHNhB6805iz2wr1C-3dkBSLGJZ5IHGSnw4WZ-md0xYJRFytOrjBOPlu1IcOQ=s0" width="300px">

4. Search for "EdgeWorkers" and hit the Install button.

5. After a moment it should be installed.

## Dependencies / Usage

1. The EdgeWorkers Explorer pane is available by going to View -> Tool Window -> EdgeWorkers Explorer. After selecting
   this it will appear on the left side of the window.

2. The EdgeWorkers IntelliJ plugin depends on the Akamai CLI, minimum version 1.3.0 with EdgeWorkers command minimum
   version 1.4.1. Many of our customers will already have this set up, but if not on first launch of the EdgeWorkers
   panel they will be prompted to install and set up OPEN API credentials in
   .edgerc [as outlined in the developer docs](https://developer.akamai.com/cli/docs/getting-started). Usage from here
   assumes you have completed this step and have met the minimum version requirement.

3. Additional configuration is available in the IntelliJ Preferences; most customers will not need to use this but if
   they use multiple .edgerc sections or a custom edgerc path they will, and Akamai internal users will likely need to
   fill in Account Key here:

<img src="https://lh5.googleusercontent.com/-lZtOKjjEfTFWZVRet_zPExUYsAvD0ycGEsyKGQaz0ajDbSCOl-lL3hqsdOYHY_mbNgxIpTgjVT28mZW16E2-4vlcLm5P5trdtX45Oi2kDbT9s43wijePnmnVhd2rCdai0ZchtsI=s0" width="300px">

## Functionality

1. List EdgeWorkers in the explorer by using the refresh button as highlighted on the left:

    <img src="https://lh3.googleusercontent.com/Rkp5PAJ-rE5yLV9aPkYlqSkNb1C3p4X7V7YO-0ckjE84i2m2YloV56gEuz17WXCNmKRWluKS5fdB2DUJIVfardUv-iHceq-MAUD18yhl6DiIkjvD_ZE1VMXU7hm2ya0VL1xeoqG-=s0" width="300px">

2. See EdgeWorker versions and version file contents by clicking through the tree:

    <img src="https://lh4.googleusercontent.com/4wxlmXuxASYgly5CLLt7kzs-Yi64aNEVnvJ2xniYM4GSxLHqs8IHGGduzl5IXpCYeBPSCDeESDhOUVEFYl_zrIaTmEC1sgBRVLkeeovdSAa-t23h73OKoQnFucOepyhW3davr7c9=s0" width="300px">

3. View file contents by clicking on them:

    <img src="https://lh6.googleusercontent.com/TnQuF2mo8Rzqk365PKSl3EZNR-LqlfCuurAukeGigxjaANdfqfwuabprARMlV3mDHk6Z93J1fA0WxNcuak1xOPe-MPa6QtWYjgLlEC-qVVmU8ZgplKU8rHJ3RvYBttKH-fqi0b9L=s0" width="300px">

4. Download a full EdgeWorker via right-click menu:

    <img src="https://lh4.googleusercontent.com/G8Y0EZMdx7Vuc5cvFarMNJQ5dQeWFFkv8e-wCsRwvzwDyePl0eKYpuXLuP1gSUaP0nWKO9KmhAOXFR8kiawsAzSiLzS-vYTXCjkYSpa4lhsgXBr3_XbXjsKCQ0BY2CzzNpPtcTE6=s0" width="300px">

5. Activate an EdgeWorker version via the same menu:

    <img src="https://lh5.googleusercontent.com/9BhJIw-pNWehlVQfO_eY8MB-prfqEcWDXlW12-eL4ZvWT7q4W5BJruX3X83fWTcELV74DUgYYKyM6TkWq0jZOgl8ps1_7lVsXdYVn60vjfZNYieTX9rbxlh-tX8LPd9vi0vPNw6f=s0" width="300px">

6. Or by using the icon at the top of the pane:

    <img src="https://lh6.googleusercontent.com/y8igV2UfShUSi_2t-wovMNhtRWzXePkZ8If15EgPuITNZDSdGWi8EPKEh7EUc_Q-v2tZF6k9XszWxMt9GCiL8DCv_FeV3RNO2mvFKWdaVLXq5tTeYYhboBC2kcBx0njbjAcw4R6S=s0" width="300px">

7. A brand new EdgeWorker can be registered by using the icon right of it:

    <img src="https://lh6.googleusercontent.com/OXblAbXPHq7DXTj457baEA_H6P4CIbmoh-ZF91qPuFHVl83h9K054wFJSa_KdLGxGx-m_8GFoxOrZ1qpujTpYuAgYL8wRaDsaSBZ7036S2VyuRN2-BJ0TnxDqrcFki-_MSvPUNdh=s0" width="300px">

    <img src="https://lh5.googleusercontent.com/_vEpv6FnGZFH55EviUwbOvOnQjSSDR1FHqENzsTqcoalCLmBbV9_j8D0BTFmGgzCc3mFwtrTVktit7PSM5OAN6cVCQ9w9BoVxVCqu25IVeu8Svt-Y99dtVPV3qKrNoZYaPJYIUa2=s0" width="300px">

8. And a new version for an existing EdgeWorker can be added via the icon left:

    <img src="https://lh5.googleusercontent.com/M069wAFrFovZ7ToJbv7oxv8nYbaeXgEpn9PHvkVUZ5t2E3Hgjll02X7vrvMWZh8kbKI9evIc741kyrjxiMFZmUNuwEDy08nRFrHIkUmj6gBC5FpqUkiSF86ZqzZyRvQpWaRuo0QU=s0" width="300px">

    <img src="https://lh5.googleusercontent.com/0XPyT1vRGYvou0zN2HnZFYAAHfrsG52v42PtwUf8DAchUxqs5Hb7PNA_PfGz5_NtjzGSGnx2r5wHicCFZsXJA0D6usGVohNM4q41G2uyx7Ni7vNz_-E3GfD9dk4KnZQZ5Ah-wQQR=s0" width="300px">

9. A new EdgeWorkers bundle/tgz can be created and automatically validated by right-clicking on an EdgeWorker
   bundle.json source file in the Project pane:

    <img src="https://lh3.googleusercontent.com/6ane23oHdzjOXm58ClhuAI6GOZ6vuWYiM9zMaufFOvTTfT9aeXk312Yai9sce_fCGgzmKsjHuAUdxy1TOD-0jqbIxmDf-bOhsJGa0RfT06Xi4vy8b8B8s9NUCW9hNyoaDIsCVQ1b=s0" width="300px">

10. And that produced tgz, or any other EdgeWorker tgz, can be tested in EdgeWorkers sandbox or uploaded as a new
    version via right click menu on the file:

    <img src="https://lh5.googleusercontent.com/TpmaNZs4hQlfe9jbkfll7Dr94aeFcempR_AchBOVCdb62-sTsZKRlb8MU9tUEvFRvBgH4-4F0juaHQKL4cupkihrIDaZB-VV3i5xG9Uc6Eh5rlOHgBSqT3Gokn_LhiNY2MkVU_4L=s0" width="300px">

11. Code profiling is available by bringing up the bottom panel. More info on profiling code with the EdgeWorkers
    Toolkit for IntelliJ, check
    out [Akamai Techdocs page on the EdgeWorkers Code Profiler](https://techdocs.akamai.com/edgeworkers/docs/edgeworkers-code-profiler).
