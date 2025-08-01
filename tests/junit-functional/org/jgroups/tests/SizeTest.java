
package org.jgroups.tests;


import org.jgroups.*;
import org.jgroups.auth.FixedMembershipToken;
import org.jgroups.auth.RegexMembership;
import org.jgroups.auth.X509Token;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.protocols.relay.RelayHeader;
import org.jgroups.protocols.relay.SiteMaster;
import org.jgroups.protocols.relay.SiteUUID;
import org.jgroups.stack.GossipData;
import org.jgroups.stack.GossipType;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.UUID;
import org.jgroups.util.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.jgroups.protocols.relay.RelayHeader.DATA;


/**
 * Tests whether method size() of a header and its serialized size correspond
 * @author  Bela Ban
 */
@Test(groups=Global.FUNCTIONAL)
public class SizeTest {


    public void testTpHeader() throws Exception {
        _testSize(new TpHeader("DemoChannel"));
        _testSize(new TpHeader("DemoChannel", (byte)1, 4));
        _testSize(new TpHeader("DemoChannel", (byte)2, 4));
    }

    public void testInfoHeader() throws Exception {
        InfoHeader hdr=new InfoHeader();
        _testSize(hdr);
        hdr.put("name", "Bela");
        _testSize(hdr);
        hdr.put("id", "322649");
        _testSize(hdr);
    }

    public void testByteArray() throws Exception {
        byte[] arr={};
        ByteArray ba=new ByteArray(arr);
        _testSize(ba);
        arr="hello world".getBytes();
        ba=new ByteArray(arr);
        _testSize(ba);
        ba=new ByteArray(arr, 6, 5);
        _testSize(ba);
    }

    public void testPingHeader() throws Exception {
        _testSize(new PingHeader(PingHeader.GET_MBRS_REQ).clusterName("bla"));
        _testSize(new PingHeader(PingHeader.GET_MBRS_RSP));
        _testSize(new PingHeader(PingHeader.GET_MBRS_RSP).clusterName(null));
        _testSize(new PingHeader(PingHeader.GET_MBRS_RSP).clusterName("cluster"));
    }
 

    public void testPingData() throws Exception {
        PingData data;
        final Address a=Util.createRandomAddress("A");
        final PhysicalAddress physical_addr=new IpAddress("127.0.0.1", 7500);

        data=new PingData(null, false);
        _testSize(data);

        data=new PingData(a, true);
        _testSize(data);

        data=new PingData(a, true, "A", physical_addr).coord(true);
        _testSize(data);

        data=new PingData(a, true, "A", physical_addr).coord(true)
          .mbrs(Arrays.asList(Util.createRandomAddress("A"), Util.createRandomAddress("B")));
        _testSize(data);

        // testing equals(), hashcode() and compareTo():
        UUID u1=new UUID(1,1), u2=new UUID(2,2), u3=new UUID(1,1);
        NameCache.add(u1, "U1"); NameCache.add(u2, "U2)"); NameCache.add(u3, "U3");
        PingData p1=new PingData(u1, true), p2=new PingData(u2, true), p3=new PingData(u3, true);

        //noinspection EqualsWithItself
        assert p1.compareTo(p1) == 0;
        assert p1.compareTo(p2) < 0;
        assert p1.compareTo(p3) == 0;

        Map<PingData,String> m=new HashMap<>(3);
        m.put(p1, "p1");
        m.put(p2, "p2");
        m.put(p3, "p3");
        assert m.size() == 2;

        Set<PingData> s=new ConcurrentSkipListSet<>(List.of(p1, p2, p3));
        assert s.size() == 2;
    }

    public static void testAuthHeader() throws Exception {
        _testSize(new AuthHeader(new FixedMembershipToken("192.168.1.5[7800],192.168.1.3[7800]")));
        _testSize(new AuthHeader(new RegexMembership()));
        X509Token tok=new X509Token().encryptedToken(new byte[]{'b', 'e', 'l', 'a'});
        _testSize(new AuthHeader(tok));
    }

    public void testGossipData() throws Exception {
        GossipData data;
        final Address own=org.jgroups.util.UUID.randomUUID();
        final Address coord=org.jgroups.util.UUID.randomUUID();
        NameCache.add(own, "own");
        NameCache.add(coord, "coord");
        PingData pd1=new PingData(coord, true, "coord", new IpAddress(7400));
        PingData pd2=new PingData(own, true, "own", new IpAddress(7500));

        final PhysicalAddress physical_addr_1=new IpAddress("127.0.0.1", 7500);

        _testSize(new GossipData(GossipType.REGISTER));

        data=new GossipData(GossipType.REGISTER, "DemoCluster", own, (List<PingData>)null, null);
        _testSize(data);

        data=new GossipData(GossipType.REGISTER, "DemoCluster", own, Arrays.asList(pd1, pd2), null);
        _testSize(data);

        data=new GossipData(GossipType.REGISTER, "DemoCluster", own, Arrays.asList(pd2, pd1), physical_addr_1);
        _testSize(data);

        data=new GossipData(GossipType.REGISTER, "demo", own, "logical_name", null);
        _testSize(data);

        data=new GossipData(GossipType.REGISTER, "demo", own, new byte[]{'b', 'e', 'l', 'a'});
        _testSize(data);

        byte[] buffer=new byte[10];
        buffer[2]='B';
        buffer[3]='e';
        buffer[4]='l';
        buffer[5]='a';
        data=new GossipData(GossipType.REGISTER, "demo", own, buffer, 2, 4);
        _testSize(data);

        buffer="hello world".getBytes();
        data=new GossipData(GossipType.MESSAGE, "demo", null, buffer, 0, buffer.length);
        _testSize(data);
    }


    public void testDigest() throws Exception {
        Address addr=Util.createRandomAddress();
        Address addr2=Util.createRandomAddress();
        View view=View.create(addr, 1, addr, addr2);
        MutableDigest mutableDigest=new MutableDigest(view.getMembersRaw());
        mutableDigest.set(addr, 200, 205);
        mutableDigest.set(addr2, 104, 105);
        _testSize(mutableDigest);

        Digest digest=new MutableDigest(view.getMembersRaw());
        _testSize(digest);
    }

    public void testNakackHeader2() throws Exception {
        _testSize(NakAckHeader2.createMessageHeader(322649));
        _testSize(NakAckHeader2.createXmitRequestHeader(Util.createRandomAddress()));
        _testSize(NakAckHeader2.createXmitResponseHeader());
    }

    public void testNakackHeader() throws Exception {
        _testSize(NakAckHeader.createMessageHeader(322649));
        _testSize(NakAckHeader.createXmitRequestHeader(Util.createRandomAddress()));
        _testSize(NakAckHeader.createXmitResponseHeader());
        _testSize(NakAckHeader.createAckHeader(322649));
    }


    public void testFdHeaders() throws Exception {
        IpAddress a1=new IpAddress("127.0.0.1", 5555);
        IpAddress a2=new IpAddress("127.0.0.1", 6666);

        FD_SOCK.FdHeader sockhdr=new FD_SOCK.FdHeader(FD_SOCK.FdHeader.GET_CACHE);
        _testSize(sockhdr);

        sockhdr=new FD_SOCK.FdHeader(FD_SOCK.FdHeader.SUSPECT, new IpAddress("127.0.0.1", 5555));
        _testSize(sockhdr);

        Set<Address> tmp=Set.of(a1, a2);
        sockhdr=new FD_SOCK.FdHeader(FD_SOCK.FdHeader.SUSPECT);
        _testSize(sockhdr);

        sockhdr.mbrs(tmp);
        _testSize(sockhdr);

        FD_SOCK2.FdHeader hdr=new FD_SOCK2.FdHeader(FD_SOCK2.FdHeader.SUSPECT);
        _testSize(hdr);
        hdr.mbrs(tmp);
        _testSize(hdr);

        hdr=new FD_SOCK2.FdHeader(FD_SOCK2.FdHeader.CONNECT_RSP);
        hdr.serverAddress(a1);
        _testSize(hdr);

        hdr.cluster("demo");
        _testSize(hdr);
    }



    public void testFdSockHeaders() throws Exception {
        FD_SOCK.FdHeader hdr=new FD_SOCK.FdHeader(FD_SOCK.FdHeader.GET_CACHE);
        _testSize(hdr);

        hdr=new FD_SOCK.FdHeader(FD_SOCK.FdHeader.GET_CACHE, new IpAddress("127.0.0.1", 4567));
        _testSize(hdr);

        Set<Address> set=Set.of(new IpAddress(3452), new IpAddress("127.0.0.1", 5000));
        hdr=new FD_SOCK.FdHeader(FD_SOCK.FdHeader.GET_CACHE).mbrs(set);
        _testSize(hdr);

        // check that IpAddress is correctly sized in FD_SOCK.FdHeader
        hdr = new FD_SOCK.FdHeader(FD_SOCK.FdHeader.I_HAVE_SOCK, new IpAddress("127.0.0.1", 4567), 
                                   new IpAddress("127.0.0.1", 4567));
        _testSize(hdr) ;
    }

    public void testUnicast3Header() throws Exception {
        UnicastHeader3 hdr=UnicastHeader3.createDataHeader(322649, (short)127, false);
        _testSize(hdr);
        _testMarshalling(hdr);

        hdr=UnicastHeader3.createDataHeader(322649, Short.MAX_VALUE, false);
        _testSize(hdr);
        _testMarshalling(hdr);

        hdr=UnicastHeader3.createDataHeader(322649, (short)(Short.MAX_VALUE -10), true);
        _testSize(hdr);
        _testMarshalling(hdr);

        //noinspection NumericOverflow
        for(long timestamp: new long[]{0, 100, Long.MAX_VALUE -1, Long.MAX_VALUE, Long.MAX_VALUE +100}) {
            hdr=UnicastHeader3.createSendFirstSeqnoHeader((int)timestamp);
            _testSize(hdr);
            _testMarshalling(hdr);
        }

        hdr=UnicastHeader3.createAckHeader(322649, (short)2, 500600);
        _testSize(hdr);
        _testMarshalling(hdr);

        hdr=UnicastHeader3.createXmitReqHeader();
        _testSize(hdr);
        _testMarshalling(hdr);
    }


    public static void testStableHeader() throws Exception {
        org.jgroups.protocols.pbcast.STABLE.StableHeader hdr;
        Address addr=UUID.randomUUID();
        View view=View.create(addr, 1, addr);

        hdr=new STABLE.StableHeader(STABLE.StableHeader.STABLE_GOSSIP, view.getViewId());
        _testSize(hdr);

        hdr=new STABLE.StableHeader(STABLE.StableHeader.STABILITY, null);
        _testSize(hdr);
    }


    public static void testSequencerHeader() throws Exception {
        org.jgroups.protocols.SEQUENCER.SequencerHeader hdr;
        hdr=new SEQUENCER.SequencerHeader((byte)1, 1L);
        _testSize(hdr);
    }


    public static void testAddressVector() throws Exception {
        List<Address> v=new ArrayList<>();
        _testSize(v);
        v.add(new IpAddress(1111));
        _testSize(v);
        v.add(new IpAddress(2222));
        _testSize(v);
    }


    public static void testViewId() throws Exception {
        ViewId vid=new ViewId();
        _testSize(vid);

        Address addr=Util.createRandomAddress("A");

        vid=new ViewId(addr);
        _testSize(vid);

        vid=new ViewId(addr, 322649);
        _testSize(vid);
    }


    public static void testMergeId() throws Exception {
        MergeId id=MergeId.create(UUID.randomUUID());
        System.out.println("id = " + id);
        _testSize(id);

        id=MergeId.create(UUID.randomUUID());
        System.out.println("id = " + id);
        _testSize(id);

        Address addr=UUID.randomUUID();
        id=MergeId.create(addr);
        System.out.println("id = " + id);
        _testSize(id);

        id=MergeId.create(addr);
        System.out.println("id = " + id);
        _testSize(id);

        id=MergeId.create(addr);
        System.out.println("id = " + id);
        _testSize(id);
    }


    public static void testView() throws Exception {
        Address one=Util.createRandomAddress("A");
        ViewId vid=new ViewId(one, 322649);
        List<Address> mbrs=new ArrayList<>();
        mbrs.add(one);
        View v=new View(vid, mbrs);
        _testSize(v);

        mbrs.add(Util.createRandomAddress("B"));
        v=new View(vid, mbrs);
        _testSize(v);

        mbrs.add(Util.createRandomAddress("C"));
        v=new View(vid, mbrs);
        _testSize(v);
    }

    public void testDeltaView() throws Exception {
        Address[] prev_mbrs=Util.createRandomAddresses(4); // A,B,C,D
        Address[] new_mbrs=Arrays.copyOf(prev_mbrs, prev_mbrs.length); // A,B,E,F (-CD +EF)
        new_mbrs[2]=Util.createRandomAddress("E");
        new_mbrs[3]=Util.createRandomAddress("F");

        View v1=View.create(prev_mbrs[0], 1, prev_mbrs);
        View v2=View.create(new_mbrs[0], 2, new_mbrs);

        Address[][] diff=View.diff(v1,v2);

        Address[] joined=diff[0], left=diff[1];
        DeltaView dv=new DeltaView(v2.getViewId(), v1.getViewId(), left, joined);
        System.out.println("dv = " + dv);
        _testSize(dv);
    }


    public static void testLargeView() throws Exception {
        Address[] members=Util.createRandomAddresses(1000);
        View view=View.create(members[0], 1, members);
        _testSize(view);


        ViewId new_view_id=new ViewId(members[0], 2);
        view=new DeltaView(new_view_id, view.getViewId(),
                           new Address[]{members[4],members[5]},
                           new Address[]{Util.createRandomAddress("new-1"), Util.createRandomAddress("new-2")});
        _testSize(view);
    }


    public static void testMergeView() throws Exception {
        ViewId vid=new ViewId(Util.createRandomAddress("A"), 322649);
        List<Address> mbrs=new ArrayList<>();
        View v=new MergeView(vid, mbrs, null);
        _testSize(v);

        mbrs.add(Util.createRandomAddress("A"));
        v=new MergeView(vid, mbrs, null);
        _testSize(v);

        mbrs.add(Util.createRandomAddress("B"));
        v=new MergeView(vid, mbrs, null);
        _testSize(v);
    }


    public void testMergeView2() throws Exception {
        Address a=Util.createRandomAddress("A"), b=Util.createRandomAddress("B"), c=Util.createRandomAddress("C"),
          d=Util.createRandomAddress("D"), e=Util.createRandomAddress("E"), f=Util.createRandomAddress("F");
        List<Address> all=Arrays.asList(a,b,c,d,e,f);
        View v1=View.create(a, 1, a,b,c);
        View v2=View.create(d, 2, d);
        View v3=View.create(e, 3, e,f);

        List<View> subgroups=List.of(v1,v2,v3);
        MergeView view_all=new MergeView(a, 5, all, subgroups);
        System.out.println("MergeView: " + view_all);
        _testSize(view_all);
    }


    public void testMergeView3() throws Exception {
        List<Address> m1, m2 , m3, all;
        List<View> subgroups;
        Address a,b,c,d,e,f;
        View v1, v2, v3, v4, view_all;

        a=new IpAddress(1000);
        b=new IpAddress(2000);
        c=new IpAddress(3000);
        d=new IpAddress(4000);
        e=new IpAddress(5000);
        f=new IpAddress(6000);

        m1=List.of(a,b,c);
        m2=List.of(d);
        m3=List.of(e,f);
        all=List.of(a,b,c,d,e,f);
        subgroups=new ArrayList<>();

        v1=new View(a, 1, m1);
        v2=new MergeView(d, 2, m2, new ArrayList<>());
        v3=new View(e, 3, m3);
        v4=new MergeView(e, 4, m3, null);
        subgroups.add(v1);
        subgroups.add(v2);
        subgroups.add(v3);
        subgroups.add(v4);

        view_all=new MergeView(a, 5, all, subgroups);
        System.out.println("MergeView: " + view_all);
        _testSize(view_all);
    }



    public void testMergeViewWithMergeViewsAsSubgroups() throws Exception {
        Address[] mbrs=Util.createRandomAddresses(4);
        Address a=mbrs[0], b=mbrs[1], c=mbrs[2], d=mbrs[3];
        View ab=new MergeView(a, 2, List.of(a,b), Arrays.asList(View.create(a, 1, a), View.create(b, 1, b)));
        View cd=new MergeView(c, 2, List.of(c,d), Arrays.asList(View.create(c, 1, c), View.create(d, 1, d)));
        MergeView abcd=new MergeView(a, 3, List.of(mbrs), List.of(ab, cd));
        _testSize(abcd);
    }


    /** Tests a MergeView whose subgroups are *not* a subset of the members (https://issues.redhat.com/browse/JGRP-1707) */
    public void testMergeViewWithNonMatchingSubgroups() throws Exception {
        Address[] mbrs=Util.createRandomAddresses(6);
        Address a=mbrs[0],b=mbrs[1],c=mbrs[2],d=mbrs[3],e=mbrs[4],f=mbrs[5];
        List<Address> abc=Arrays.asList(a,b,c);     // A,B,C
        List<Address> def=Arrays.asList(d,e,f);  // D,E,F
        List<Address> full=Arrays.asList(a,b,c,d,f); // E is missing
        List<View> subviews=Arrays.asList(new View(a, 4, abc), new View(d, 4, def));
        MergeView mv=new MergeView(a, 5, full, subviews);
        MergeView tmp_view=(MergeView)_testSize(mv);
        assert mv.deepEquals(tmp_view) : "views don't match: original=" + mv + ", new=" + tmp_view;

        full=Arrays.asList(a,b,c,e,f); // D (creator!) is missing
        subviews=Arrays.asList(new View(a, 4, abc), new View(d, 4, def));
        mv=new MergeView(a, 5, full, subviews);
        tmp_view=(MergeView)_testSize(mv);
        assert mv.deepEquals(tmp_view) : "views don't match: original=" + mv + ", new=" + tmp_view;
    }




    public void testLargeMergeView() throws Exception {
        int NUM=100;
        Address[] members=Util.createRandomAddresses(NUM, true);
        Address[] first=Arrays.copyOf(members,NUM / 2);
        Address[] second=new Address[NUM/2];
        System.arraycopy(members, NUM / 2, second, 0, second.length);

        View v1=View.create(first[0], 5, first), v2=View.create(second[0], 5, second);
        MergeView mv=new MergeView(new ViewId(first[0], 6), members, Arrays.asList(v1, v2));
        _testSize(mv);
    }


    public void testMergeHeader() throws Exception {
        MERGE3.MergeHeader hdr=new MERGE3.MergeHeader();
        _testSize(hdr);
        ViewId view_id=new ViewId(Util.createRandomAddress("A"), 22);
        hdr=MERGE3.MergeHeader.createInfo(view_id, null, null);
        _testSize(hdr);
        String logical_name="A";
        hdr=MERGE3.MergeHeader.createInfo(view_id, logical_name, null);
        _testSize(hdr);
        PhysicalAddress physical_addr=new IpAddress(5002);
        hdr=MERGE3.MergeHeader.createInfo(view_id, logical_name, physical_addr);
        _testSize(hdr);
        hdr=MERGE3.MergeHeader.createViewRequest();
        _testSize(hdr);
        hdr=MERGE3.MergeHeader.createViewResponse();
        _testSize(hdr);
    }


    public void testJoinRsp() throws Exception {
        JoinRsp rsp;
        Address a=Util.createRandomAddress("A"), b=Util.createRandomAddress("B"), c=Util.createRandomAddress("C");
        View v=View.create(a, 55, a, b, c);

        MutableDigest digest=new MutableDigest(v.getMembersRaw());
        digest.set(a, 1000, 1050);
        digest.set(b, 700, 700);
        digest.set(c, 0, 0);
        rsp=new JoinRsp(v, digest);
        _testSize(rsp);

        rsp=new JoinRsp(v, null);
        _testSize(rsp);

        rsp=new JoinRsp("boom");
        _testSize(rsp);
    }

    public void testLargeJoinRsp() throws Exception {
        int NUM=1000;
        Address[] members=new Address[NUM];
        for(int i=0; i < members.length; i++)
            members[i]=Util.createRandomAddress("m" + i);

        View view=View.create(members[0], 53, members);
        MutableDigest digest=new MutableDigest(view.getMembersRaw());
        for(Address member: members)
            digest.set(member, 70000, 100000);

        JoinRsp rsp=new JoinRsp(view, digest);
        _testSize(rsp);

        rsp=new JoinRsp(view, null);
        _testSize(rsp);
    }


    public void testGmsHeader() throws Exception {
        Address addr=UUID.randomUUID();
        GMS.GmsHeader hdr=new GMS.GmsHeader(GMS.GmsHeader.JOIN_REQ, addr);
        _testSize(hdr);

        hdr=new GMS.GmsHeader(GMS.GmsHeader.JOIN_RSP);
        _testSize(hdr);

        hdr=new GMS.GmsHeader(GMS.GmsHeader.MERGE_REQ);
        _testSize(hdr);

        Address[] addresses=Util.createRandomAddresses(20);
        View      view=View.create(addresses[0], 1, addresses);
        MutableDigest digest=new MutableDigest(view.getMembersRaw());
        for(int i=0; i < addresses.length; i++) {
            long hd=i + 10000;
            digest.set(addresses[i], hd, hd + 500);
        }
        hdr=new GMS.GmsHeader(GMS.GmsHeader.MERGE_RSP);
        _testSize(hdr);

        view=View.create(addresses[0],1,addresses);
        digest=new MutableDigest(addresses); // no ref to view.members
        for(int i=0; i < addresses.length; i++) {
            long hd=i + 10000;
            digest.set(addresses[i], hd, hd + 500);
        }
        hdr=new GMS.GmsHeader(GMS.GmsHeader.MERGE_RSP);
        _testSize(hdr);
    }


    public void testFCHeader() throws Exception {
        FcHeader hdr=new FcHeader(FcHeader.REPLENISH);
        _testSize(hdr);
    }


    public void testFragHeader() throws Exception {
        FragHeader hdr=new FragHeader(322649, 1, 10);
        _testSize(hdr);
        hdr.needsDeserialization(true);
        _testSize(hdr);
    }

    public void testFragHeader3() throws Exception {
        Frag3Header hdr=new Frag3Header(322649, 1, 10);
        _testSize(hdr);
        hdr.needsDeserialization(true);
        _testSize(hdr);

        hdr=new Frag3Header(322649, 2, 10, 10000, 3000);
        _testSize(hdr);
    }

    public void testCompressHeader() throws Exception {
        COMPRESS.CompressHeader hdr=new COMPRESS.CompressHeader(2002);
        _testSize(hdr);
    }


    public static void testStompHeader() throws Exception {
        STOMP.StompHeader hdr=STOMP.StompHeader.createHeader(STOMP.StompHeader.Type.MESSAGE,
                                                             "destination", "/topics/chat",
                                                             "sender", UUID.randomUUID().toString());
        _testSize(hdr);

        hdr=STOMP.StompHeader.createHeader(STOMP.StompHeader.Type.ENDPOINT, "endpoint", "192.168.1.5:8787");
        _testSize(hdr);
    }


    public static void testStateHeader() throws Exception {
        STATE_TRANSFER.StateHeader hdr=new STATE_TRANSFER.StateHeader(STATE_TRANSFER.StateHeader.STATE_REQ, null);
        _testSize(hdr);
    }


    public void testRelayHeader() throws Exception {
        Address dest=new SiteMaster("sfo");
        RelayHeader hdr=new RelayHeader(DATA, dest, null);
        _testSize(hdr);
        Address sender=new SiteUUID(UUID.randomUUID(), "dummy", "sfo");
        hdr=new RelayHeader(DATA, dest, sender);
        _testSize(hdr);

        hdr=new RelayHeader(RelayHeader.SITES_UP, null, null)
          .addToSites("sfo", "lon", "nyc");
        _testSize(hdr);

        hdr=new RelayHeader(DATA, dest, null)
          .addToSites("sfo")
          .addToVisitedSites(List.of("nyc", "sfc", "lon"));
        _testSize(hdr);

        Header[] hdrs=headers();
        hdr.originalHeaders(hdrs);
        _testSize(hdr);

        RelayHeader hdr2=hdr.copy();
        assert hdr.getType() == hdr2.getType();
        assert hdr.getSites().equals(hdr2.getSites());
        assert hdr.getVisitedSites().equals(hdr2.getVisitedSites());
    }

    public void testSiteUUID() throws Exception {
        SiteUUID u1=new SiteUUID((UUID)Util.createRandomAddress(), "A", "sfo");
        _testSize(u1);

        SiteMaster sm=new SiteMaster("sfo");
        _testSize(sm);
    }

    public void testEncryptHeader() throws Exception {
        EncryptHeader hdr=new EncryptHeader((byte)0, new byte[]{'b','e', 'l', 'a'}, new byte[]{'b', 'a', 'n'});
        _testSize(hdr);
        hdr=new EncryptHeader((byte)0, "Hello".getBytes(), "World".getBytes());
        _testSize(hdr);
    }


    public void testIpAddress() throws Exception {
        IpAddress addr=new IpAddress();
        _testSize(addr);
    }


    public static void testIpAddress1() throws Exception {
        IpAddress addr=new IpAddress("127.0.0.1", 5555);
        _testSize(addr);
    }


    public static void testIpAddressWithHighPort() throws Exception {
        IpAddress addr=new IpAddress("127.0.0.1", 65535);
        _testSize(addr);
    }


    public static void testIpAddress2() throws Exception {
        IpAddress addr=new IpAddress(3456);
        _testSize(addr);
    }


    public static void testIpAddress3() throws Exception {
        IpAddress addr=new IpAddress((String)null, 5555);
        _testSize(addr);
    }


    public static void testIpAddress4() throws Exception {
        IpAddress addr=new IpAddress((InetAddress)null, 5555);
        _testSize(addr);
    }


    public static void testWriteAddress() throws Exception {
        Address uuid=UUID.randomUUID();
        _testWriteAddress(uuid);

        Address addr=new IpAddress(7500);
        _testWriteAddress(addr);

        addr=new IpAddress("127.0.0.1", 5678);
        _testWriteAddress(addr);

        addr=new SiteMaster("sfo");
        _testWriteAddress(addr);
    }

    public void testWriteAddresses() throws Exception {
        List<Address> list=new ArrayList<>();
        for(int i=0; i < 3; i++)
            list.add(UUID.randomUUID());
        _testWriteAddresses(list);

        list.clear();
        list.add(new IpAddress(7500));
        list.add(new IpAddress("192.168.1.5", 4444));
        list.add(new IpAddress("127.0.0.1", 5674));
        _testWriteAddresses(list);
    }

    public void testUUID() throws Exception {
        org.jgroups.util.UUID uuid=org.jgroups.util.UUID.randomUUID();
        System.out.println("uuid = " + uuid);
        _testSize(uuid);

        uuid=org.jgroups.util.UUID.randomUUID();
        byte[] buf=Util.streamableToByteBuffer(uuid);
        org.jgroups.util.UUID uuid2=Util.streamableFromByteBuffer(UUID::new, buf);
        System.out.println("uuid:  " + uuid);
        System.out.println("uuid2: " + uuid2);
        assert uuid.equals(uuid2);

        int hash1=uuid.hashCode(), hash2=uuid2.hashCode();
        System.out.println("hash 1: " + hash1);
        System.out.println("hash 2: " + hash2);
        assert hash1 == hash2;
    }

    public void testRequestCorrelatorHeader() throws Exception {
        RequestCorrelator.Header hdr;

        hdr=new RequestCorrelator.Header(RequestCorrelator.Header.REQ, 0, (short)1000);
        _testSize(hdr);

        hdr=new RequestCorrelator.Header(RequestCorrelator.Header.RSP, 322649, (short)356);

        ByteArrayOutputStream output=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(output);
        hdr.writeTo(out);
        out.flush();

        byte[] buf=output.toByteArray();
        out.close();

        ByteArrayInputStream input=new ByteArrayInputStream(buf);
        DataInputStream in=new DataInputStream(input);

        hdr=new RequestCorrelator.Header();
        hdr.readFrom(in);

        Assert.assertEquals(hdr.req_id, 322649);
        assert hdr.rspExpected();
        Assert.assertEquals(hdr.corrId, (short)356);
        Assert.assertEquals(hdr.type, RequestCorrelator.Header.RSP);

        hdr=new RequestCorrelator.Header(RequestCorrelator.Header.RSP, 322649, (short)356);

        output=new ByteArrayOutputStream();
        out=new DataOutputStream(output);
        hdr.writeTo(out);
        out.flush();

        buf=output.toByteArray();
        out.close();

        input=new ByteArrayInputStream(buf);
        in=new DataInputStream(input);

        hdr=new RequestCorrelator.Header();
        hdr.readFrom(in);

        Assert.assertEquals(hdr.req_id, 322649);
        assert hdr.rspExpected();
        Assert.assertEquals(hdr.corrId, 356);
        Assert.assertEquals(hdr.type, RequestCorrelator.Header.RSP);

        Address a=Util.createRandomAddress("A"), b=Util.createRandomAddress("B");

        hdr=new RequestCorrelator.MultiDestinationHeader(RequestCorrelator.Header.REQ, 322649, (short)22, new Address[]{a,b});
        _testSize(hdr);
    }

    public void testDhHeader() throws Exception {
        byte[] dh_key={'p','u','b','l','i','c'};
        byte[] encr_secret={'s','e','c','r','e','t'};
        byte[] version={'v','e','r','s','i','o','n'};
        DH_KEY_EXCHANGE.DhHeader hdr=DH_KEY_EXCHANGE.DhHeader.createSecretKeyRequest(dh_key);
        _test(hdr);

        hdr=DH_KEY_EXCHANGE.DhHeader.createSecretKeyResponse(dh_key, encr_secret, version);
        _test(hdr);
    }

    protected static void _test(DH_KEY_EXCHANGE.DhHeader hdr) throws Exception {
        int expected_size=hdr.serializedSize();
        byte[] buf=Util.streamableToByteBuffer(hdr);
        assert buf.length == expected_size;

        DH_KEY_EXCHANGE.DhHeader hdr2=Util.streamableFromByteBuffer(DH_KEY_EXCHANGE.DhHeader::new, buf, 0, buf.length);
        assert Arrays.equals(hdr.dhKey(), hdr2.dhKey());
    }

    private static void _testWriteAddresses(List<Address> list) throws Exception {
        int len=Util.size(list);
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(output);
        Util.writeAddresses(list, out);
        out.flush();
        byte[] buf=output.toByteArray();
        out.close();

        System.out.println("\nlen=" + len + ", serialized length=" + buf.length);
        assert len == buf.length;
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(buf));
        Collection<Address> new_list=Util.readAddresses(in, ArrayList::new);
        System.out.println("old list=" + list + "\nnew list=" + new_list);
        assert list.equals(new_list);
    }

    private static void _testWriteAddress(Address addr) throws Exception {
        int len=Util.size(addr);
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(output);
        Util.writeAddress(addr, out);
        out.flush();
        byte[] buf=output.toByteArray();
        out.close();

        System.out.println("\nlen=" + len + ", serialized length=" + buf.length);
        assert len == buf.length;
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(buf));
        Address new_addr=Util.readAddress(in);
        System.out.println("old addr=" + addr + "\nnew addr=" + new_addr);
        assert addr.equals(new_addr);
    }

    private static void _testMarshalling(UnicastHeader3 hdr) throws Exception {
        byte[] buf=Util.streamableToByteBuffer(hdr);
        UnicastHeader3 hdr2=Util.streamableFromByteBuffer(UnicastHeader3::new, buf);

        assert hdr.type()       == hdr2.type();
        assert hdr.seqno()      == hdr2.seqno();
        assert hdr.connId()     == hdr2.connId();
        assert hdr.first()      == hdr2.first();
        assert hdr.timestamp()  == hdr.timestamp();
    }

    private static void _testSize(Digest digest) throws Exception {
        long len=digest.serializedSize(true);
        byte[] serialized_form=Util.streamableToByteBuffer(digest);
        System.out.println("digest = " + digest);
        System.out.println("size=" + len + ", serialized size=" + serialized_form.length);
        assert len == serialized_form.length;
    }

    private static void _testSize(Header hdr) throws Exception {
        long size=hdr.serializedSize();
        byte[] serialized_form=Util.streamableToByteBuffer(hdr);
        System.out.println(hdr.getClass().getSimpleName() + ": size=" + size + ", serialized size=" + serialized_form.length);
        assert serialized_form.length == size;
        Header hdr2=Util.streamableFromByteBuffer(hdr.getClass(), serialized_form);
        assert hdr2.serializedSize() == hdr.serializedSize();
    }

    private static void _testSize(Address addr) throws Exception {
        long size=addr.serializedSize();
        byte[] serialized_form=Util.streamableToByteBuffer(addr);
        System.out.println("size=" + size + ", serialized size=" + serialized_form.length);
        Assert.assertEquals(serialized_form.length, size);
        Address addr2=Util.streamableFromByteBuffer(addr.getClass(), serialized_form);
        assert addr.equals(addr2);
    }


    private static void _testSize(ViewId vid) throws Exception {
        long size=vid.serializedSize();
        byte[] serialized_form=Util.streamableToByteBuffer(vid);
        System.out.println("size=" + size + ", serialized size=" + serialized_form.length);
        Assert.assertEquals(serialized_form.length, size);
    }

    private static void _testSize(MergeId id) throws Exception {
        long size=id.size();
        byte[] serialized_form=Util.streamableToByteBuffer(id);
        System.out.println("size=" + size + ", serialized size=" + serialized_form.length);
        assert serialized_form.length == size;
    }


    private static View _testSize(View v) throws Exception {
        long size=v.serializedSize();
        byte[] serialized_form=Util.streamableToByteBuffer(v);
        System.out.println("size=" + size + ", serialized size=" + serialized_form.length);
        Assert.assertEquals(serialized_form.length, size);

        View view=Util.streamableFromByteBuffer(v.getClass(), serialized_form);
        System.out.println("old view: " + v + "\nnew view: " + view);
        assert view.equals(v);
        return view;
    }

    private static void _testSize(Collection<Address> coll) throws Exception {
        int size=Util.size(coll);
        byte[] serialized_form=Util.collectionToByteBuffer(coll);
        System.out.println("size=" + size + ", serialized size=" + serialized_form.length);
        Assert.assertEquals(serialized_form.length, size);
    }

    private static void _testSize(MERGE3.MergeHeader hdr) throws Exception {
        long size=hdr.serializedSize();
        byte[] serialized_form=Util.streamableToByteBuffer(hdr);
        System.out.println("size=" + size + ", serialized size=" + serialized_form.length);
        Assert.assertEquals(serialized_form.length, size);
    }

    private static void _testSize(JoinRsp rsp) throws Exception {
        long size=rsp.serializedSize();
        byte[] serialized_form=Util.streamableToByteBuffer(rsp);
        System.out.println("size=" + size + ", serialized size=" + serialized_form.length);
        Assert.assertEquals(serialized_form.length, size);

        JoinRsp rsp2=Util.streamableFromByteBuffer(JoinRsp::new, serialized_form);
        assert Util.match(rsp.getDigest(), rsp2.getDigest());
        assert Util.match(rsp.getView(), rsp2.getView());
        assert Util.match(rsp.getFailReason(), rsp2.getFailReason());
    }

    private static void _testSize(SizeStreamable data) throws Exception {
        System.out.println("\ndata: " + data);
        long size=data.serializedSize();
        byte[] serialized_form=Util.streamableToByteBuffer(data);
        System.out.println("size=" + size + ", serialized size=" + serialized_form.length);
        assert serialized_form.length == size : "serialized length=" + serialized_form.length + ", size=" + size;
    }

    private static Header[] headers() {
        return new Header[] {
          UnicastHeader3.createDataHeader(322649L, (short)1, false),
          new RequestCorrelator.Header((byte)1, 33L, (short)2),
          new FORK.ForkHeader("foo", "bar")
        };
    }

}
