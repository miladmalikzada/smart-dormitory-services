# ADR-0005: Observer for events; Factory Method + Decorator + Adapter for channels

Status: accepted · Date: week 10

**Context:** Announcements and ticket changes must reach several channels, and we need to add channels later (email, push). The SMS vendor API differs from ours.

**Options:** (A) services call channels directly; (B) EventPublisher with listeners, ChannelFactory (Factory Method) wrapping every channel in TimestampedChannel (Decorator), SmsChannelAdapter (Adapter).

**Decision:** B. Services only publish DormEvent. A new channel is one new ChannelFactory subclass plus one wiring line in App (ExtensionPointTest shows no existing class changes). No event-bus library is used.

**Consequences:** More small classes; the composition root line is the only edited place for a new channel.
