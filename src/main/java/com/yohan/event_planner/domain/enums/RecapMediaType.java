package com.yohan.event_planner.domain.enums;

/**
 * Enumeration of supported media types for event recap content.
 * 
 * <p>This enum defines the types of media files that can be attached to event recaps,
 * supporting common multimedia formats used in event documentation and sharing.</p>
 * 
 * <h2>Supported Media Types</h2>
 * <ul>
 *   <li><strong>IMAGE</strong>: Photos and visual content (JPEG, PNG, etc.)</li>
 *   <li><strong>VIDEO</strong>: Video recordings and clips</li>
 *   <li><strong>AUDIO</strong>: Voice notes, speeches, music recordings</li>
 * </ul>
 * 
 * @see com.yohan.event_planner.domain.RecapMedia
 */
public enum RecapMediaType {
    
    /**
     * Static image content including photos, screenshots, diagrams, and graphics.
     * <p>Supports common image formats and provides visual documentation of events.
     * Typically used for capturing moments, displaying results, or providing visual context.</p>
     */
    IMAGE,
    
    /**
     * Video content including recordings, clips, presentations, and visual media.
     * <p>Enables rich multimedia documentation with both visual and audio components.
     * Duration tracking is particularly useful for video content analytics.</p>
     */
    VIDEO,
    
    /**
     * Audio content including voice notes, speeches, music, and sound recordings.
     * <p>Provides audio-only documentation for events, interviews, or presentations.
     * Duration metadata helps with content organization and time tracking.</p>
     */
    AUDIO
}